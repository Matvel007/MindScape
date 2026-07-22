package com.mindscape.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AiContextBuilder {
    private AiContextBuilder() {
    }

    public static String categoriesSummary(List<Category> categories, List<Note> notes, String noCategoriesLabel) {
        if (categories.isEmpty()) return noCategoriesLabel + "\n";
        StringBuilder sb = new StringBuilder();
        List<Category> centers = KnowledgeTreeService.getCenters(categories);
        List<Category> legacy = KnowledgeTreeService.getLegacyTopLevel(categories);
        if (centers.isEmpty() && legacy.isEmpty()) return noCategoriesLabel + "\n";

        for (int i = 0; i < centers.size(); i++) {
            Category center = centers.get(i);
            String tag = (i == 0) ? " [ГЛАВНЫЙ ЦЕНТР, #" + (i + 1) + "]" : " [Центр #" + (i + 1) + "]";
            int[] cnt = KnowledgeTreeService.countContents(categories, notes, center.fullPath());
            sb.append("● ").append(center.title).append(tag)
                    .append(" (подкатегорий: ").append(cnt[0]).append(", заметок: ").append(cnt[1]).append(")\n");
            appendCategoryTree(sb, categories, notes, center.fullPath(), 1);
        }
        for (Category category : legacy) {
            int[] cnt = KnowledgeTreeService.countContents(categories, notes, category.fullPath());
            sb.append("○ ").append(category.title).append(" (папка без центра)")
                    .append(" (подкатегорий: ").append(cnt[0]).append(", заметок: ").append(cnt[1]).append(")\n");
            appendCategoryTree(sb, categories, notes, category.fullPath(), 1);
        }
        return sb.toString();
    }

    public static String notesSummary(List<Note> notes, String noNotesLabel) {
        if (notes.isEmpty()) return noNotesLabel + "\n";
        StringBuilder sb = new StringBuilder();
        int id = 1;
        for (Note note : notes) {
            sb.append(id).append(". ").append(note.title);
            if (note.quickContainerNote) {
                sb.append(" [быстрая заметка из контейнера]");
            }
            if (note.categoryPath != null && !note.categoryPath.isEmpty()) {
                sb.append(" [").append(note.categoryPath).append("]");
            }
            sb.append("\n");
            id++;
        }
        return sb.toString();
    }

    private static void appendCategoryTree(StringBuilder sb, List<Category> categories, List<Note> notes, String parentPath, int depth) {
        List<Category> children = new ArrayList<>();
        for (Category category : categories) {
            if (parentPath.equals(category.parent)) children.add(category);
        }
        children.sort(Comparator.comparing(c -> c.title.toLowerCase(Locale.ROOT)));
        for (Category child : children) {
            for (int i = 0; i < depth; i++) sb.append("  ");
            int noteCount = 0;
            for (Note note : notes) {
                if (child.fullPath().equals(note.categoryPath)) noteCount++;
            }
            sb.append("├ ").append(child.title).append(" [").append(child.fullPath()).append("]");
            if (noteCount > 0) sb.append(" (заметок: ").append(noteCount).append(")");
            sb.append("\n");
            appendCategoryTree(sb, categories, notes, child.fullPath(), depth + 1);
        }
    }
}
