/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonpatch.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonNumEquals;
import com.github.fge.jackson.jsonpointer.JsonPointerCustom;
import com.github.fge.jsonpatch.Iterables;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchOperation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: cleanup
final class DiffProcessor {
    private static final JsonNumEquals EQUIVALENCE
            = JsonNumEquals.getInstance();

    private final Map<JsonPointerCustom, JsonNode> unchanged;

    public final List<DiffOperation> diffs = new ArrayList<DiffOperation>();

    DiffProcessor(final Map<JsonPointerCustom, JsonNode> unchanged) {
        this.unchanged = Collections.unmodifiableMap(new HashMap<JsonPointerCustom, JsonNode>(unchanged));
    }

    void valueReplaced(final JsonPointerCustom pointer, final JsonNode oldValue,
                       final JsonNode newValue) {
        diffs.add(DiffOperation.replace(pointer, oldValue, newValue));
    }

    void valueRemoved(final JsonPointerCustom pointer, final JsonNode value) {
        diffs.add(DiffOperation.remove(pointer, value));
    }

    void valueAdded(final JsonPointerCustom pointer, final JsonNode value) {
        final int removalIndex = findPreviouslyRemoved(value);
        if (removalIndex != -1) {
            final DiffOperation removed = diffs.get(removalIndex);
            diffs.remove(removalIndex);
            diffs.add(DiffOperation.move(removed.getFrom(),
                    value, pointer, value));
            return;
        }
        final JsonPointerCustom ptr = findUnchangedValue(value);
        final DiffOperation op = ptr != null
                ? DiffOperation.copy(ptr, pointer, value)
                : DiffOperation.add(pointer, value);

        diffs.add(DiffOperation.add(pointer, value));
    }

    JsonPatch getPatch() {
        final List<JsonPatchOperation> list = new ArrayList<JsonPatchOperation>();

        for (final DiffOperation op : diffs)
            list.add(op.asJsonPatchOperation());

        return new JsonPatch(list);
    }

    @Nullable
    private JsonPointerCustom findUnchangedValue(final JsonNode value) {
        for (final Map.Entry<JsonPointerCustom, JsonNode> entry : unchanged.entrySet())
            if (EQUIVALENCE.equivalent(value, entry.getValue()))
                return entry.getKey();
        return null;
    }

    private int findPreviouslyRemoved(final JsonNode value) {
        DiffOperation op;

        for (int i = 0; i < diffs.size(); i++) {
            op = diffs.get(i);
            if (op.getType() == DiffOperation.Type.REMOVE
                    && EQUIVALENCE.equivalent(value, op.getOldValue()))
                return i;
        }
        return -1;
    }

/** start code here */

    /**
     * original value added into valueReplaced method at last.
     */
    void valueReplaced2(final JsonPointerCustom pointer, final JsonNode oldValue,
                        final JsonNode newValue, final JsonNode originalValue)
    {
        diffs.add(DiffOperation.replace2(pointer,oldValue,newValue,originalValue));
    }

    /**
     * original value added into valueRemove method at last.
     */
    void valueRemoved2(final JsonPointerCustom pointer, final JsonNode oldvalue, final JsonNode originalValue) {
        diffs.add(DiffOperation.remove2(pointer,null,originalValue));
    }

    /**
     * original value added into valueAdded method at last.
     */
    void valueAdded2(final JsonPointerCustom pointer, final JsonNode value, final JsonNode originalValue) {
//        final int removalIndex = findPreviouslyRemoved(value);
//        if (removalIndex != -1) {
//            final DiffOperation removed = diffs.get(removalIndex);
//            diffs.remove(removalIndex);
//            diffs.add(DiffOperation.move(removed.getFrom(), value, pointer, value));
//            return;
//        }
//        final JsonPointerCustom ptr = findUnchangedValue(value);
//        final DiffOperation op = ptr != null
//                ? DiffOperation.copy(ptr, pointer, value)
//                : DiffOperation.add2(pointer, value, originalValue);

        diffs.add(DiffOperation.add2(pointer, originalValue,originalValue));
    }


}
