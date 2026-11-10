package com.dozingcatsoftware.vectorpinball.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dozingcatsoftware.vectorpinball.editor.elements.EditableField;

public class UndoStack {

    static class Entry {
        Map<String, Object> fieldMap;
        Set<Integer> selectedIndexes;
    }

    EditableField editableField;
    List<Entry> entries = new ArrayList<>();
    int currentEntryIndex = -1;

    public void setEditableField(EditableField field) {
        editableField = field;
    }

    public void clearStack() {
        entries.clear();
        currentEntryIndex = -1;
    }

    public void pushSnapshot() {
        Map<String, Object> fieldMapSnapshot = editableField.getPropertyMapSnapshot();
        if (currentEntryIndex<0 || !entries.get(currentEntryIndex).fieldMap.equals(fieldMapSnapshot)) {
            // Save the currently selected items, so that if we undo we can restore them.
            if (currentEntryIndex >= 0) {
                Entry currentEntry = entries.get(currentEntryIndex);
                currentEntry.selectedIndexes = editableField.getSelectedElementIndexes();
            }
            Entry entry = new Entry();
            entry.fieldMap = fieldMapSnapshot;
            while (entries.size() > currentEntryIndex+1) {
                entries.remove(entries.size()-1);
            }
            entries.add(entry);
            currentEntryIndex += 1;
        }
    }

    public boolean canUndo() {
        return currentEntryIndex>0;
    }

    public boolean canRedo() {
        return entries.size()>0 && currentEntryIndex<entries.size()-1;
    }

    public void undo() {
        if (!canUndo()) {
            throw new IllegalStateException("Can't undo");
        }
        currentEntryIndex -= 1;
        restoreFromCurrentIndex();
    }

    public void redo() {
        if (!canRedo()) {
            throw new IllegalStateException("Can't undo");
        }
        currentEntryIndex += 1;
        restoreFromCurrentIndex();
    }

    void restoreFromCurrentIndex() {
        Entry topEntry = entries.get(currentEntryIndex);
        editableField.initFromProperties(topEntry.fieldMap);
        if (topEntry.selectedIndexes != null) {
            editableField.setSelectedElementIndexes(topEntry.selectedIndexes);
        }
    }
}
