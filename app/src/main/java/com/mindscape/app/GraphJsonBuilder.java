package com.mindscape.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GraphJsonBuilder {
    interface HiddenNodeResolver {
        boolean isHidden(Object entity);
    }

    private static final float ROOT_CLUSTER_RADIUS = 150f;
    private static final float CHILD_BRANCH_GAP = 24f;
    private static final float CHILD_BRANCH_SPACING = 24f;
    private static final float MULTI_CENTER_CHILD_FAN = (float) (Math.PI * 0.85);
    private static final float NESTED_BRANCH_FAN = (float) (Math.PI * 1.45);
    private static final float SINGLE_CHILD_TURN_MIN = 1.35f;
    private static final float SINGLE_CHILD_TURN_RANGE = 0.35f;

    private static final class LayoutResult {
        List<Float> distances = new ArrayList<>();
        List<Float> angles = new ArrayList<>();
    }

    private GraphJsonBuilder() {
    }

    static String build(
            List<Category> categoriesList,
            List<Note> notesList,
            List<LocalFileLink> localFilesList,
            List<Connection> connectionsList,
            HiddenNodeResolver hiddenNodeResolver,
            String rootNodeTitle
    ) {
        try {
            JSONArray jNodes = new JSONArray();
            JSONArray jEdges = new JSONArray();
            Map<String, List<Category>> childrenOf = new HashMap<>();
            Map<String, float[]> nodePos = new HashMap<>();
            Set<String> graphNodeIds = new HashSet<>();
            for (Category category : categoriesList) graphNodeIds.add("folder:" + category.fullPath());
            for (Note note : notesList) graphNodeIds.add("note:" + note.fullPath());
            for (LocalFileLink file : localFilesList) graphNodeIds.add(file.nodeId());

            List<Category> topLevel = new ArrayList<>();
            List<Category> centers = new ArrayList<>();
            for (Category category : categoriesList) {
                if (category.parent == null || category.parent.isEmpty()) {
                    topLevel.add(category);
                    if (category.isCenter) centers.add(category);
                }
                String parentKey = category.parent == null ? "" : category.parent;
                childrenOf.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(category);
            }

            List<Category> legacyTopLevel = new ArrayList<>();
            for (Category category : topLevel) {
                if (!category.isCenter) legacyTopLevel.add(category);
            }

            final boolean useCenters = !centers.isEmpty();
            final List<Category> rootHubs = useCenters ? centers : topLevel;

            Map<String, Integer> noteCountByCat = new HashMap<>();
            for (Note note : notesList) {
                if (note.isUnbound()) continue;
                String path = note.categoryPath != null ? note.categoryPath : note.deepestCategory();
                int currentVal = noteCountByCat.getOrDefault(path, 0);
                noteCountByCat.put(path, currentVal + 1);
            }
            Map<String, Integer> fileCountByCat = new HashMap<>();
            for (LocalFileLink file : localFilesList) {
                if (file.isUnbound()) continue;
                int currentVal = fileCountByCat.getOrDefault(file.folderPath, 0);
                fileCountByCat.put(file.folderPath, currentVal + 1);
                noteCountByCat.put(file.folderPath, noteCountByCat.getOrDefault(file.folderPath, 0) + 1);
            }

            Map<String, Float> orbitRadii = new HashMap<>();
            for (Category category : categoriesList) {
                getOrComputeOrbit(category, childrenOf, noteCountByCat, orbitRadii);
            }

            if (useCenters) {
                LayoutResult lr = calculateHubLayout(centers.size());
                for (int i = 0; i < centers.size(); i++) {
                    Category center = centers.get(i);
                    float ang = lr.angles.get(i);
                    float dist = lr.distances.get(i);
                    float cx = (float) (Math.cos(ang) * dist);
                    float cy = (float) (Math.sin(ang) * dist);
                    nodePos.put(center.fullPath(), new float[]{cx, cy});

                    JSONObject jn = new JSONObject();
                    jn.put("id", "folder:" + center.fullPath());
                    jn.put("title", center.title);
                    jn.put("group", center.fullPath());
                    jn.put("level", -1);
                    jn.put("depth", 0);
                    jn.put("x", (int) cx);
                    jn.put("y", (int) cy);
                    jn.put("hidden", hiddenNodeResolver.isHidden(center));
                    jNodes.put(jn);

                    List<Category> kids = childrenOf.getOrDefault(center.fullPath(), new ArrayList<>());
                    if (!kids.isEmpty()) {
                        float rBase = getCategoryBaseRadius(center.fullPath(), noteCountByCat);
                        LayoutResult lrKids = centers.size() == 1
                                ? calculateCircleLayout(kids, orbitRadii, rBase, CHILD_BRANCH_SPACING)
                                : calculateSectorLayout(kids, orbitRadii, rBase, CHILD_BRANCH_SPACING, ang, MULTI_CENTER_CHILD_FAN);
                        for (int j = 0; j < kids.size(); j++) {
                            Category child = kids.get(j);
                            float ca = lrKids.angles.get(j);
                            float kDist = lrKids.distances.get(j);
                            float kx = cx + (float) (Math.cos(ca) * kDist);
                            float ky = cy + (float) (Math.sin(ca) * kDist);
                            placeCategory(
                                    child, kx, ky, 1, ca, childrenOf, orbitRadii, nodePos, jNodes, jEdges,
                                    "folder:" + center.fullPath(), child.fullPath(), notesList, hiddenNodeResolver
                            );
                        }
                    }
                }

                if (!legacyTopLevel.isEmpty()) {
                    float rcMax = ROOT_CLUSTER_RADIUS;
                    for (int i = 0; i < centers.size(); i++) {
                        float dist = lr.distances.get(i);
                        float orbit = orbitRadii.getOrDefault(centers.get(i).fullPath(), 50f);
                        if (dist + orbit > rcMax) {
                            rcMax = dist + orbit;
                        }
                    }
                    float roBase = rcMax + 150f;
                    LayoutResult lrLegacy = calculateCircleLayout(legacyTopLevel, orbitRadii, roBase, 50f);
                    for (int i = 0; i < legacyTopLevel.size(); i++) {
                        Category category = legacyTopLevel.get(i);
                        float ang = lrLegacy.angles.get(i);
                        float dist = lrLegacy.distances.get(i);
                        float cx = (float) (Math.cos(ang) * dist);
                        float cy = (float) (Math.sin(ang) * dist);
                        placeCategory(
                                category, cx, cy, 1, ang, childrenOf, orbitRadii, nodePos, jNodes, jEdges,
                                rootNodeTitle, category.fullPath(), notesList, hiddenNodeResolver
                        );
                    }
                }
            } else {
                LayoutResult lr = calculateHubLayout(topLevel.size());
                for (int i = 0; i < topLevel.size(); i++) {
                    Category category = topLevel.get(i);
                    float angle0 = lr.angles.get(i);
                    float dist = lr.distances.get(i);
                    float cx = (float) (Math.cos(angle0) * dist);
                    float cy = (float) (Math.sin(angle0) * dist);
                    nodePos.put(category.fullPath(), new float[]{cx, cy});

                    JSONObject jn = new JSONObject();
                    jn.put("id", "folder:" + category.fullPath());
                    jn.put("title", category.title);
                    jn.put("group", category.fullPath());
                    jn.put("level", -1);
                    jn.put("depth", 0);
                    jn.put("x", (int) cx);
                    jn.put("y", (int) cy);
                    jn.put("hidden", hiddenNodeResolver.isHidden(category));
                    jNodes.put(jn);

                    placeCategory(
                            category, cx, cy, 1, angle0, childrenOf, orbitRadii, nodePos, jNodes, jEdges,
                            "folder:" + category.fullPath(), category.fullPath(), notesList, hiddenNodeResolver
                    );
                }
            }

            Map<String, Integer> itemIdxByParent = new HashMap<>();
            for (Note note : notesList) {
                if (note.isUnbound()) continue;
                String path = note.categoryPath != null ? note.categoryPath : note.deepestCategory();
                float[] pPos = nodePos.get(path);
                if (pPos == null) pPos = nodePos.getOrDefault(note.deepestCategory(), new float[]{0, 0});

                String noteEdgeSrc = note.categoryPath != null ? note.categoryPath : note.deepestCategory();
                int idx = itemIdxByParent.getOrDefault(path, 0);
                int total = noteCountByCat.getOrDefault(path, 1);

                float noteSpan = (float) (Math.PI * 2.0);
                float nStep = total <= 1 ? 0 : noteSpan / total;
                float nAngle = idx * nStep + (float) (Math.PI / 4);

                float r2 = Math.max(58f, total * 9f);
                float nx = pPos[0] + (float) (Math.cos(nAngle) * r2);
                float ny = pPos[1] + (float) (Math.sin(nAngle) * r2);
                itemIdxByParent.put(path, idx + 1);

                String groupName = path;
                for (Category hub : rootHubs) {
                    String hubPath = hub.fullPath();
                    if (note.categoryPath != null && (note.categoryPath.equals(hubPath) || note.categoryPath.startsWith(hubPath + "/"))) {
                        String resolved = hubPath;
                        for (Category top : childrenOf.getOrDefault(hubPath, java.util.Collections.emptyList())) {
                            String topPath = top.fullPath();
                            if (note.categoryPath.equals(topPath) || note.categoryPath.startsWith(topPath + "/")) {
                                resolved = topPath;
                                break;
                            }
                        }
                        groupName = resolved;
                        break;
                    }
                }
                JSONObject jn = new JSONObject();
                jn.put("id", "note:" + note.fullPath());
                jn.put("title", note.title);
                jn.put("group", groupName);
                jn.put("level", 3);
                jn.put("x", (int) nx);
                jn.put("y", (int) ny);
                jn.put("hidden", hiddenNodeResolver.isHidden(note));
                jNodes.put(jn);

                JSONObject ne = new JSONObject();
                ne.put("source", "folder:" + noteEdgeSrc);
                ne.put("target", "note:" + note.fullPath());
                jEdges.put(ne);
            }

            for (LocalFileLink file : localFilesList) {
                String path = file.folderPath;
                String parentEdgeSource = null;
                float[] pPos;

                if (file.isUnbound()) {
                    Category anchor = rootHubs.isEmpty() ? null : rootHubs.get(0);
                    String anchorPath = anchor == null ? "local-files" : anchor.fullPath();
                    parentEdgeSource = anchor == null ? null : "folder:" + anchorPath;
                    path = anchorPath;
                    pPos = anchor == null ? new float[]{0, 0} : nodePos.getOrDefault(anchorPath, new float[]{0, 0});
                } else {
                    parentEdgeSource = "folder:" + path;
                    pPos = nodePos.getOrDefault(path, new float[]{0, 0});
                }

                int idx = itemIdxByParent.getOrDefault(path, 0);
                int total = file.isUnbound()
                        ? Math.max(1, localFilesList.size())
                        : noteCountByCat.getOrDefault(path, 1);
                float fileSpan = (float) (Math.PI * 2.0);
                float fStep = total <= 1 ? 0 : fileSpan / total;
                float fAngle = idx * fStep + (float) (Math.PI / 4);
                float r2 = Math.max(58f, total * 9f);
                float fx = pPos[0] + (float) (Math.cos(fAngle) * r2);
                float fy = pPos[1] + (float) (Math.sin(fAngle) * r2);
                itemIdxByParent.put(path, idx + 1);

                String groupName = path;
                for (Category hub : rootHubs) {
                    String hubPath = hub.fullPath();
                    if (path.equals(hubPath) || path.startsWith(hubPath + "/")) {
                        groupName = hubPath;
                        break;
                    }
                }

                JSONObject jn = new JSONObject();
                jn.put("id", file.nodeId());
                jn.put("title", file.title);
                jn.put("group", groupName);
                jn.put("level", 4);
                jn.put("type", "file");
                jn.put("mimeType", file.mimeType);
                jn.put("fileTitle", file.title);
                jn.put("x", (int) fx);
                jn.put("y", (int) fy);
                jn.put("hidden", hiddenNodeResolver.isHidden(file));
                jNodes.put(jn);

                JSONObject fe = new JSONObject();
                if (parentEdgeSource != null) {
                    fe.put("source", parentEdgeSource);
                    fe.put("target", file.nodeId());
                    jEdges.put(fe);
                }
            }

            for (Connection conn : connectionsList) {
                String source = NodeStateManager.normalizeConnectionNodeId(conn.source);
                String target = NodeStateManager.normalizeConnectionNodeId(conn.target);
                if (!graphNodeIds.contains(source) || !graphNodeIds.contains(target)) continue;
                if (isFolderItemConnection(source, target)) continue;
                JSONObject ce = new JSONObject();
                ce.put("source", source);
                ce.put("target", target);
                jEdges.put(ce);
            }

            JSONObject graph = new JSONObject();
            graph.put("nodes", jNodes);
            graph.put("edges", jEdges);

            List<Category> allCenters = getCenters(categoriesList);
            String mainCenterId;
            float[] mc;
            if (allCenters.size() > 1) {
                mainCenterId = rootNodeTitle;
                mc = new float[]{0, 0};
            } else if (!allCenters.isEmpty()) {
                mainCenterId = allCenters.get(0).fullPath();
                mc = nodePos.getOrDefault(mainCenterId, new float[]{0, 0});
            } else if (!legacyTopLevel.isEmpty()) {
                mainCenterId = legacyTopLevel.get(0).fullPath();
                mc = nodePos.getOrDefault(mainCenterId, new float[]{0, 0});
            } else {
                mainCenterId = rootNodeTitle;
                mc = new float[]{0, 0};
            }
            JSONObject mcObj = new JSONObject();
            mcObj.put("id", mainCenterId);
            mcObj.put("x", (int) mc[0]);
            mcObj.put("y", (int) mc[1]);
            graph.put("mainCenter", mcObj);
            return graph.toString();
        } catch (Exception e) {
            return "{\"nodes\":[],\"edges\":[]}";
        }
    }

    private static boolean isFolderItemConnection(String source, String target) {
        boolean sourceFolder = source != null && source.startsWith("folder:");
        boolean targetFolder = target != null && target.startsWith("folder:");
        boolean sourceItem = source != null && (source.startsWith("file:") || source.startsWith("note:"));
        boolean targetItem = target != null && (target.startsWith("file:") || target.startsWith("note:"));
        return (sourceFolder && targetItem) || (targetFolder && sourceItem);
    }

    private static float getCategoryBaseRadius(String path, Map<String, Integer> noteCountByCat) {
        int noteCount = noteCountByCat.getOrDefault(path, 0);
        if (noteCount > 0) {
            float r2 = Math.max(58f, noteCount * 9f);
            return r2 + 24f;
        }
        return 50f;
    }

    private static LayoutResult calculateHubLayout(int count) {
        LayoutResult res = new LayoutResult();
        if (count <= 0) return res;
        if (count == 1) {
            res.distances.add(0f);
            res.angles.add(0f);
            return res;
        }

        float radius = ROOT_CLUSTER_RADIUS + Math.max(0, count - 3) * 18f;
        float startAngle = -(float) (Math.PI / 2.0);
        for (int i = 0; i < count; i++) {
            res.distances.add(radius);
            res.angles.add(startAngle + (float) (Math.PI * 2.0 * i / count));
        }
        return res;
    }

    private static LayoutResult calculateCircleLayout(List<Category> items, Map<String, Float> orbitRadii, float rBase, float spacing) {
        LayoutResult res = new LayoutResult();
        if (items.isEmpty()) return res;

        List<Float> itemOrbits = new ArrayList<>();
        List<Float> nominalDistances = new ArrayList<>();
        for (Category item : items) {
            float orbit = orbitRadii.getOrDefault(item.fullPath(), 50f);
            itemOrbits.add(orbit);
            nominalDistances.add(rBase + orbit);
        }

        float totalArcSpan = 0f;
        List<Float> angleSteps = new ArrayList<>();
        if (items.size() > 1) {
            for (int i = 0; i < items.size(); i++) {
                float o1 = itemOrbits.get(i);
                float o2 = itemOrbits.get((i + 1) % items.size());
                float d1 = nominalDistances.get(i);
                float d2 = nominalDistances.get((i + 1) % items.size());
                float dAvg = (d1 + d2) / 2f;
                float w = o1 + o2 + spacing;
                float step = w / dAvg;
                angleSteps.add(step);
                totalArcSpan += step;
            }
        }

        float scaleFactor = 1.0f;
        float maxArcSpan = (float) (2.0 * Math.PI);
        if (totalArcSpan > maxArcSpan) {
            scaleFactor = totalArcSpan / maxArcSpan;
        }

        for (float nd : nominalDistances) {
            res.distances.add(nd * scaleFactor);
        }

        if (items.size() == 1) {
            res.angles.add(-(float) (Math.PI / 2));
        } else {
            float angleScale = (float) (2.0 * Math.PI) / (totalArcSpan / scaleFactor);
            float currentAngle = -(float) (Math.PI / 2);
            for (int i = 0; i < items.size(); i++) {
                res.angles.add(currentAngle);
                if (i < angleSteps.size()) {
                    currentAngle += (angleSteps.get(i) / scaleFactor) * angleScale;
                }
            }
        }
        return res;
    }

    private static LayoutResult calculateSectorLayout(
            List<Category> items,
            Map<String, Float> orbitRadii,
            float rBase,
            float spacing,
            float centerAngle,
            float maxArcSpan
    ) {
        LayoutResult res = new LayoutResult();
        if (items.isEmpty()) return res;

        List<Float> itemOrbits = new ArrayList<>();
        List<Float> nominalDistances = new ArrayList<>();
        for (Category item : items) {
            float orbit = orbitRadii.getOrDefault(item.fullPath(), 50f);
            itemOrbits.add(orbit);
            nominalDistances.add(rBase + orbit + CHILD_BRANCH_GAP);
        }

        float totalArcSpan = 0f;
        List<Float> angleSteps = new ArrayList<>();
        if (items.size() > 1) {
            for (int i = 0; i < items.size() - 1; i++) {
                float o1 = itemOrbits.get(i);
                float o2 = itemOrbits.get(i + 1);
                float d1 = nominalDistances.get(i);
                float d2 = nominalDistances.get(i + 1);
                float dAvg = (d1 + d2) / 2f;
                float w = o1 + o2 + spacing;
                float step = w / dAvg;
                angleSteps.add(step);
                totalArcSpan += step;
            }
        }

        float scaleFactor = 1.0f;
        if (totalArcSpan > maxArcSpan) {
            scaleFactor = totalArcSpan / maxArcSpan;
        }

        for (float nd : nominalDistances) {
            res.distances.add(nd * scaleFactor);
        }

        if (items.size() == 1) {
            res.angles.add(centerAngle);
        } else {
            float actualArcSpan = totalArcSpan / scaleFactor;
            float currentAngle = centerAngle - actualArcSpan / 2f;
            for (int i = 0; i < items.size(); i++) {
                res.angles.add(currentAngle);
                if (i < angleSteps.size()) {
                    currentAngle += angleSteps.get(i) / scaleFactor;
                }
            }
        }
        return res;
    }

    private static float getOrComputeOrbit(
            Category cat,
            Map<String, List<Category>> childrenOf,
            Map<String, Integer> noteCountByCat,
            Map<String, Float> memo
    ) {
        String path = cat.fullPath();
        if (memo.containsKey(path)) {
            return memo.get(path);
        }
        float rBase = getCategoryBaseRadius(path, noteCountByCat);

        List<Category> children = childrenOf.getOrDefault(path, null);
        if (children == null || children.isEmpty()) {
            memo.put(path, rBase);
            return rBase;
        }

        boolean is360 = cat.isCenter;
        float maxArcSpan = is360 ? (float) (Math.PI * 2.0) : NESTED_BRANCH_FAN;

        List<Float> childOrbits = new ArrayList<>();
        List<Float> nominalDistances = new ArrayList<>();
        for (Category child : children) {
            float childOrbit = getOrComputeOrbit(child, childrenOf, noteCountByCat, memo);
            childOrbits.add(childOrbit);
            nominalDistances.add(rBase + childOrbit + CHILD_BRANCH_GAP);
        }

        float totalArcSpan = 0f;
        if (children.size() > 1) {
            int limit = is360 ? children.size() : children.size() - 1;
            for (int i = 0; i < limit; i++) {
                float o1 = childOrbits.get(i);
                float o2 = childOrbits.get((i + 1) % children.size());
                float d1 = nominalDistances.get(i);
                float d2 = nominalDistances.get((i + 1) % children.size());
                float dAvg = (d1 + d2) / 2f;
                float w = o1 + o2 + CHILD_BRANCH_SPACING;
                totalArcSpan += w / dAvg;
            }
        }

        float scaleFactor = 1.0f;
        if (totalArcSpan > maxArcSpan) {
            scaleFactor = totalArcSpan / maxArcSpan;
        }

        float maxChildOuter = rBase;
        for (int i = 0; i < children.size(); i++) {
            float childOrbit = childOrbits.get(i);
            float d = nominalDistances.get(i) * scaleFactor;
            float outer = d + childOrbit;
            if (outer > maxChildOuter) {
                maxChildOuter = outer;
            }
        }

        memo.put(path, maxChildOuter);
        return maxChildOuter;
    }

    private static void placeCategory(
            Category cat,
            float cx,
            float cy,
            int depth,
            float incomingAngle,
            Map<String, List<Category>> childrenOf,
            Map<String, Float> orbitRadii,
            Map<String, float[]> nodePos,
            JSONArray jNodes,
            JSONArray jEdges,
            String parentId,
            String rootGroupName,
            List<Note> notesList,
            HiddenNodeResolver hiddenNodeResolver
    ) throws Exception {
        nodePos.put(cat.fullPath(), new float[]{cx, cy});

        int level = Math.min(depth, 2);
        JSONObject jn = new JSONObject();
        jn.put("id", "folder:" + cat.fullPath());
        jn.put("title", cat.title);
        jn.put("group", rootGroupName);
        jn.put("level", level);
        jn.put("x", (int) cx);
        jn.put("y", (int) cy);
        jn.put("depth", depth);
        jn.put("hidden", hiddenNodeResolver.isHidden(cat));
        jNodes.put(jn);

        JSONObject edge = new JSONObject();
        edge.put("source", parentId);
        edge.put("target", "folder:" + cat.fullPath());
        jEdges.put(edge);

        List<Category> children = childrenOf.getOrDefault(cat.fullPath(), new ArrayList<>());
        if (children.isEmpty()) return;

        boolean is360 = cat.isCenter;
        float maxArcSpan = is360 ? (float) (Math.PI * 2.0) : NESTED_BRANCH_FAN;

        List<Float> childOrbits = new ArrayList<>();
        List<Float> nominalDistances = new ArrayList<>();

        int noteCount = 0;
        for (Note note : notesList) {
            if (note.isUnbound()) continue;
            String path = note.categoryPath != null ? note.categoryPath : note.deepestCategory();
            if (path.equals(cat.fullPath())) {
                noteCount++;
            }
        }
        float rBase = noteCount > 0 ? Math.max(58f, noteCount * 9f) + 24f : 50f;

        for (Category child : children) {
            float childOrbit = orbitRadii.getOrDefault(child.fullPath(), 50f);
            childOrbits.add(childOrbit);
            nominalDistances.add(rBase + childOrbit + CHILD_BRANCH_GAP);
        }

        float totalArcSpan = 0f;
        List<Float> angleSteps = new ArrayList<>();
        if (children.size() > 1) {
            int limit = is360 ? children.size() : children.size() - 1;
            for (int i = 0; i < limit; i++) {
                float o1 = childOrbits.get(i);
                float o2 = childOrbits.get((i + 1) % children.size());
                float d1 = nominalDistances.get(i);
                float d2 = nominalDistances.get((i + 1) % children.size());
                float dAvg = (d1 + d2) / 2f;
                float w = o1 + o2 + CHILD_BRANCH_SPACING;
                float step = w / dAvg;
                angleSteps.add(step);
                totalArcSpan += step;
            }
        }

        float scaleFactor = 1.0f;
        if (totalArcSpan > maxArcSpan) {
            scaleFactor = totalArcSpan / maxArcSpan;
        }

        List<Float> actualDistances = new ArrayList<>();
        for (float nd : nominalDistances) {
            actualDistances.add(nd * scaleFactor);
        }

        float baseAngle = is360 ? incomingAngle : branchBaseAngle(cat, incomingAngle, depth);
        List<Float> angles = new ArrayList<>();
        if (children.size() == 1) {
            angles.add(singleChildAngle(cat, incomingAngle, depth));
        } else if (is360) {
            float angleScale = (float) (2.0 * Math.PI) / (totalArcSpan / scaleFactor);
            float currentAngle = baseAngle;
            for (int i = 0; i < children.size(); i++) {
                angles.add(currentAngle);
                if (i < angleSteps.size()) {
                    currentAngle += (angleSteps.get(i) / scaleFactor) * angleScale;
                }
            }
        } else {
            float actualArcSpan = totalArcSpan / scaleFactor;
            float currentAngle = baseAngle - actualArcSpan / 2f;
            for (int i = 0; i < children.size(); i++) {
                angles.add(currentAngle);
                if (i < angleSteps.size()) {
                    currentAngle += angleSteps.get(i) / scaleFactor;
                }
            }
        }

        for (int j = 0; j < children.size(); j++) {
            Category child = children.get(j);
            float angle = angles.get(j);
            float dist = actualDistances.get(j);

            float scx = cx + (float) (Math.cos(angle) * dist);
            float scy = cy + (float) (Math.sin(angle) * dist);

            placeCategory(
                    child, scx, scy, depth + 1, angle, childrenOf, orbitRadii, nodePos, jNodes, jEdges,
                    "folder:" + cat.fullPath(), rootGroupName, notesList, hiddenNodeResolver
            );
        }
    }

    private static float branchBaseAngle(Category category, float incomingAngle, int depth) {
        float offset = deterministicUnit(category.fullPath() + ":fan:" + depth) * 0.75f;
        return incomingAngle + offset;
    }

    private static float singleChildAngle(Category category, float incomingAngle, int depth) {
        float unit = deterministicUnit(category.fullPath() + ":single:" + depth);
        float sign = unit >= 0 ? 1f : -1f;
        float magnitude = SINGLE_CHILD_TURN_MIN + Math.abs(unit) * SINGLE_CHILD_TURN_RANGE;
        return incomingAngle + sign * magnitude;
    }

    private static float deterministicUnit(String value) {
        int hash = value.hashCode();
        int positive = hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
        return (positive % 2001) / 1000f - 1f;
    }

    private static List<Category> getCenters(List<Category> categoriesList) {
        List<Category> result = new ArrayList<>();
        for (Category category : categoriesList) {
            if (category.isCenter && (category.parent == null || category.parent.isEmpty())) {
                result.add(category);
            }
        }
        return result;
    }
}
