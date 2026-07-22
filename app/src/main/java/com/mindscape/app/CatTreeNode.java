package com.mindscape.app;

import java.util.ArrayList;
import java.util.List;

public final class CatTreeNode {
    public String name;
    public String fullPath;
    public List<CatTreeNode> children = new ArrayList<>();

    public CatTreeNode(String name, String fullPath) {
        this.name = name;
        this.fullPath = fullPath;
    }
}
