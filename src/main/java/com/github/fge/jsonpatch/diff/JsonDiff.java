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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonNumEquals;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointerCustom;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchMessages;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.json.JsonPointer;
import javax.swing.text.html.ObjectView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * JSON "diff" implementation
 *
 * <p>This class generates a JSON Patch (as in, an RFC 6902 JSON Patch) given
 * two JSON values as inputs. The patch can be obtained directly as a {@link
 * JsonPatch} or as a {@link JsonNode}.</p>
 *
 * <p>Note: there is <b>no guarantee</b> about the usability of the generated
 * patch for any other source/target combination than the one used to generate
 * the patch.</p>
 *
 * <p>This class always performs operations in the following order: removals,
 * additions and replacements. It then factors removal/addition pairs into
 * move operations, or copy operations if a common element exists, at the same
 * {@link JsonPointerCustom pointer}, in both the source and destination.</p>
 *
 * <p>You can obtain a diff either as a {@link JsonPatch} directly or, for
 * backwards compatibility, as a {@link JsonNode}.</p>
 *
 * @since 1.2
 */
@ParametersAreNonnullByDefault
public final class JsonDiff {
    private static final MessageBundle BUNDLE
            = MessageBundles.getBundle(JsonPatchMessages.class);
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();

    private static final JsonNumEquals EQUIVALENCE
            = JsonNumEquals.getInstance();

    private JsonDiff() {
    }

    /**
     * Generate a JSON patch for transforming the source node into the target
     * node
     *
     * @param source the node to be patched
     * @param target the expected result after applying the patch
     * @return the patch as a {@link JsonPatch}
     * @since 1.9
     */
    public static JsonPatch asJsonPatch(final JsonNode source,
                                        final JsonNode target) {
        BUNDLE.checkNotNull(source, "common.nullArgument");
        BUNDLE.checkNotNull(target, "common.nullArgument");

        final Map<JsonPointerCustom, JsonNode> unchanged
                = getUnchangedValues(source, target);

        final DiffProcessor processor = new DiffProcessor(unchanged);

        generateDiffs(processor, JsonPointerCustom.empty(), source, target);

        return processor.getPatch();
    }

    public static JsonPatch asJsonPatch(final JsonNode source,
                                        final JsonNode target, final Map<JsonPointerCustom, Set<String>> map) {
        BUNDLE.checkNotNull(source, "common.nullArgument");
        BUNDLE.checkNotNull(target, "common.nullArgument");

        final Map<JsonPointerCustom, JsonNode> unchanged = computeNonChanged(source, target);

        DiffProcessor diffProcessor = new DiffProcessor(unchanged);

        generateDiffs2(diffProcessor, JsonPointerCustom.empty(), source, target);

        return diffProcessor.getPatch();
    }

    private static void generateDiffs2(DiffProcessor diffProcessor, JsonPointerCustom pointer, JsonNode source, JsonNode target) {
        //converting source and target into map
        Map<String, Object> source1 = new ObjectMapper().convertValue(source, new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> target1 = new ObjectMapper().convertValue(target, new TypeReference<Map<String, Object>>() {
        });

        boolean foundObj = false;

        //loop for source to get each and every element
        for (Map.Entry<String, Object> ele1 : source1.entrySet()) {

            foundObj = false;

            //loop for target to get each and every element
            for (Map.Entry<String, Object> ele2 : target1.entrySet()) {

                //check source first node is what? object or array
                if (NodeType.getNodeType(new ObjectMapper().valueToTree(ele1)) == NodeType.OBJECT && NodeType.getNodeType(new ObjectMapper().valueToTree(ele1.getValue())) == NodeType.ARRAY) {
                    //check target first node is what? object or array
                    if (NodeType.getNodeType(new ObjectMapper().valueToTree(ele2)) == NodeType.OBJECT &&
                            NodeType.getNodeType(new ObjectMapper().valueToTree(ele2.getValue())) == NodeType.ARRAY) {

                        //check ele1 keys and ele2 keys by equals method
                        if (ele1.getKey().equals(ele2.getKey())) {
                            foundObj = true;
                            //check ele1 value and ele2 value by equals method
                            if (ele1.getValue().equals(ele2.getValue())) {
                                break;
                            } else {
                                //TODO change key
                                ArrayNode node1 = (ArrayNode) source.get(ele1.getKey());
                                //TODO change key
                                ArrayNode node2 = (ArrayNode) target.get(ele2.getKey());

                                /** operation start from here */
                                //check add op
                                boolean found;
                                for (int i = 0; i < node1.size(); i++) {
                                    //check if
                                    found = false;
                                    for (int j = 0; j < node2.size(); j++) {
                                        JsonNode one = node1.get(i);
                                        JsonNode two = node2.get(j);
                                        if (isEqual(one, two)) {
                                            found = true;
                                            //here we do replace operation
                                            calculateReplace(one, two, diffProcessor, pointer.append(JsonPointerCustom.of(ele1.getKey())));
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        // if not found do remove operation and add "?" in pointer
                                        diffProcessor.valueRemoved2(pointer.append(JsonPointerCustom.of(ele1.getKey()).append("?")),
                                                new ObjectMapper().convertValue(node1.get(i), JsonNode.class),
                                                new ObjectMapper().convertValue(node1.get(i), JsonNode.class));
                                    }
                                }

                                //check remove op
                                found = false;
                                for (int i = 0; i < node2.size(); i++) {
                                    //check if
                                    found = false;
                                    for (int j = 0; j < node1.size(); j++) {
                                        JsonNode one = node2.get(i);
                                        JsonNode two = node1.get(j);
                                        if (isEqual(one, two)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        // System.out.println("remove");
                                        diffProcessor.valueAdded2(pointer.append(JsonPointerCustom.of(ele1.getKey()).append("-")), node2.get(i), node2.get(i));
                                    }
                                }

                                //check replace op
//                                for (int i = 0; i < node1.size(); i++) {
//                                    //check if
//                                    for (int j = 0; j < node2.size(); j++) {
//                                        if (isEqual(node1.get(i), node2.get(j))) {
//                                            JsonNode one = node1.get(i);
//                                            JsonNode two = node1.get(j);
//                                        }
//                                    }
//                                }
//                                //here
//                                if (!found) {
//                                    diffProcessor.valueAdded(pointer.append("Entitlements"), new ObjectMapper().valueToTree(ele1));
//                                }

                            }
                        }
                    }
                } else {
                    //if key and values both are same then do nothing
                    if (ele1.getKey().equals(ele2.getKey())) {
                        foundObj = true;
                        if (ele1.getValue().equals(ele2.getValue())) {
                            break;
                        } else {
                            // System.out.println("replace");
                            diffProcessor.valueReplaced2(JsonPointerCustom.of("/", ele1.getKey()),
                                    new ObjectMapper().convertValue(ele2.getValue(), JsonNode.class),
                                    new ObjectMapper().convertValue(ele2.getValue(), JsonNode.class),
                                    new ObjectMapper().convertValue(ele1.getValue(), JsonNode.class));
                            break;
                        }
                    }
                }
            }

            if (!foundObj) {
                // if found then simply add into diff
                diffProcessor.valueAdded(pointer.append(JsonPointerCustom.of(ele1).append(JsonPointerCustom.of("-"))), new ObjectMapper().convertValue(ele1.getValue(), JsonNode.class));
            }
        }

        Map<String, Object> n1 = new ObjectMapper().convertValue(source, new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> n2 = new ObjectMapper().convertValue(target, new TypeReference<Map<String, Object>>() {
        });

        Set s1 = n1.keySet();
        Set s2 = n2.keySet();
        //here we remove set from s2
        s2.removeAll(s1);
        Iterator<String> iterate = s2.iterator();
        while (iterate.hasNext()) {
            // System.out.println("remove");
            String s = iterate.next();
            diffProcessor.valueRemoved2(pointer.append(JsonPointerCustom.of(s)),
                    new ObjectMapper().convertValue(n2.get(s), JsonNode.class),
                    new ObjectMapper().convertValue(n2.get(s), JsonNode.class));
        }

//        public final List<DiffOperation> diffs = new ArrayList<DiffOperation>();

        for (DiffOperation ele : diffProcessor.diffs) {
            // System.out.println(ele.asJsonPatchOperation());
        }
    }//end

    private static Map<JsonPointerCustom, JsonNode> computeNonChanged(JsonNode source, JsonNode target) {
        final Map<JsonPointerCustom, JsonNode> ret = new HashMap<>();
        JsonPointerCustom jsonPointer = JsonPointerCustom.of("Entitlements");
        computeNonChangedValues(ret, jsonPointer, source, target);
        return ret;
    }

    private static void computeNonChangedValues(Map<JsonPointerCustom, JsonNode> ret, JsonPointerCustom pointer, JsonNode source, JsonNode target) {

        final Iterator<String> firstFields1 = source.fieldNames();
        String name1 = firstFields1.next();

        Map<String, Object> source1 = new ObjectMapper().convertValue(source, new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> target1 = new ObjectMapper().convertValue(target, new TypeReference<Map<String, Object>>() {
        });


        boolean foundObj = false;

        for (Map.Entry<String, Object> ele1 : source1.entrySet()) {
            foundObj = false;
            for (Map.Entry<String, Object> ele2 : target1.entrySet()) {

                if (NodeType.getNodeType(new ObjectMapper().valueToTree(ele1)) == NodeType.OBJECT) {

                    if (NodeType.getNodeType(new ObjectMapper().valueToTree(ele2)) == NodeType.OBJECT) {
                        if (ele1.getValue().equals(ele2.getValue())) {
                            foundObj = true;
                            ret.put(JsonPointerCustom.of("/", ele1.getKey()), new ObjectMapper().convertValue(ele1, JsonNode.class));
                        }
                    } else {

                    }

                } else {
                    if (NodeType.getNodeType(new ObjectMapper().valueToTree(ele2)) == NodeType.ARRAY) {

                        if (ele1.getKey() == ele2.getKey()) {
                            if (NodeType.getNodeType(new ObjectMapper().valueToTree(ele1)) == NodeType.ARRAY) {


                                ArrayNode node1 = (ArrayNode) source.get(ele1.getKey());
                                ArrayNode node2 = (ArrayNode) target.get(ele2.getKey());

                                if (node1.equals(node2)) {
                                    ret.put(JsonPointerCustom.of("/", ele1.getKey()), new ObjectMapper().convertValue(ele1, JsonNode.class));
                                }
//
//                                //check replace op
//                                for (int i = 0; i < node1.size(); i++) {
//                                    //check if
//                                    for (int j = 0; j < node2.size(); j++) {
//                                        if (isEqual(node1.get(i), node2.get(j))) {
//                                            ret.put(pointer, source.get(i));
//                                        }
//                                    }
//                                }


//                                //TODO change key
//                                ArrayNode node1 = (ArrayNode) source.get(ele1.getKey());
//                                //TODO change key
//                                ArrayNode node2 = (ArrayNode) target.get(ele2.getKey());
//                                //check add op
//                                boolean found;
//                                for (int i = 0; i < node1.size(); i++) {
//                                    //check if
//                                    found = false;
//                                    for (int j = 0; j < node2.size(); j++) {
//                                        if (isEqual(node1.get(i), node2.get(j))) {
//                                            found = true;
//                                            break;
//                                        }
//                                    }
//                                    if (!found) {
//                                        System.out.println("add");
//
//                                    }
//                                }
//
//                                //check remove op
//                                found = false;
//                                for (int i = 0; i < node2.size(); i++) {
//                                    //check if
//                                    found = false;
//                                    for (int j = 0; j < node1.size(); j++) {
//                                        if (isEqual(node2.get(i), node1.get(j))) {
//                                            found = true;
//                                            break;
//                                        }
//                                    }
//                                    if (!found) {
//                                        System.out.println("remove");
//                                    }
//                                }
//
//                                //check replace op
//                                for (int i = 0; i < node1.size(); i++) {
//                                    //check if
//                                    for (int j = 0; j < node2.size(); j++) {
//                                        if (isEqual(node1.get(i), node2.get(j))) {
//                                        }
//                                    }
//                                }
//                                //here
//                                if (!found) {
//                                }

                            }
                        }

                    }
                }
            }
            if (!foundObj) {
            }
        }


    }


    private static void calculateReplace(JsonNode one, JsonNode two, DiffProcessor diffProcessor, JsonPointerCustom pointer) {
        Map<String, Object> map1 = new ObjectMapper().convertValue(one, new TypeReference<Map<String, Object>>() {
        });
        Map<String, Object> map2 = new ObjectMapper().convertValue(two, new TypeReference<Map<String, Object>>() {
        });

        for (Map.Entry<String, Object> entry : map1.entrySet()) {
            String currentKey = entry.getKey();
            if (!map1.get(currentKey).equals(map2.get(currentKey))) {
                // System.out.println("replace");
                diffProcessor.valueReplaced2(pointer.append("?").append(currentKey),
                        new ObjectMapper().convertValue(map1.get(currentKey), JsonNode.class),
                        new ObjectMapper().convertValue(map2.get(currentKey), JsonNode.class),
                        new ObjectMapper().convertValue(new ObjectMapper().convertValue(map1, JsonNode.class), JsonNode.class));
            }
        }
    }

    private static boolean isEqual(JsonNode jsonNode, JsonNode jsonNode1) {
        if (jsonNode.get("Application Key").equals(jsonNode1.get("Application Key")) &&
                jsonNode.get("Entitlement Type").equals(jsonNode1.get("Entitlement Type")) &&
                jsonNode.get("Entitlement Name").equals(jsonNode1.get("Entitlement Name"))) {
            return true;
        }
        return false;
    }

    public static JsonPatch asJsonPatchWith(final JsonNode source,
                                            final JsonNode target) {
        BUNDLE.checkNotNull(source, "common.nullArgument");
        BUNDLE.checkNotNull(target, "common.nullArgument");
        final Map<JsonPointerCustom, JsonNode> unchanged
                = getUnchangedValues(source, target);
        final DiffProcessor processor = new DiffProcessor(unchanged);

        generateDiffs(processor, JsonPointerCustom.empty(), source, target);
        return processor.getPatch();
    }

    /**
     * Generate a JSON patch for transforming the source node into the target
     * node
     *
     * @param source the node to be patched
     * @param target the expected result after applying the patch
     * @return the patch as a {@link JsonNode}
     */
    public static JsonNode asJson(final JsonNode source, final JsonNode target, final Map<JsonPointerCustom, Set<String>> map) {
        final String s;
        try {
            s = MAPPER.writeValueAsString(asJsonPatch(source, target, map));
            return MAPPER.readTree(s);
        } catch (IOException e) {
            throw new RuntimeException("cannot generate JSON diff", e);
        }
    }

    private static void generateDiffs(final DiffProcessor processor,
                                      final JsonPointerCustom pointer, final JsonNode source, final JsonNode target) {
        if (EQUIVALENCE.equivalent(source, target))
            return;

        final NodeType firstType = NodeType.getNodeType(source);
        final NodeType secondType = NodeType.getNodeType(target);

        /*
         * Node types differ: generate a replacement operation.
         */
        if (firstType != secondType) {
            processor.valueReplaced(pointer, source, target);
            return;
        }

        /*
         * If we reach this point, it means that both nodes are the same type,
         * but are not equivalent.
         *
         * If this is not a container, generate a replace operation.
         */
        if (!source.isContainerNode()) {
            processor.valueReplaced(pointer, source, target);
            return;
        }

        /*
         * If we reach this point, both nodes are either objects or arrays;
         * delegate.
         */
        if (firstType == NodeType.OBJECT)
            generateObjectDiffs(processor, pointer, (ObjectNode) source,
                    (ObjectNode) target);
        else // array
            generateArrayDiffs(processor, pointer, (ArrayNode) source,
                    (ArrayNode) target);
    }

    private static void generateObjectDiffs(final DiffProcessor processor,
                                            final JsonPointerCustom pointer, final ObjectNode source,
                                            final ObjectNode target) {
        final Set<String> firstFields
                = collect(source.fieldNames(), new TreeSet<String>());

        final Set<String> secondFields
                = collect(target.fieldNames(), new TreeSet<String>());

        final Set<String> copy1 = new HashSet<String>(firstFields);
        copy1.removeAll(secondFields);

        for (final String field : Collections.unmodifiableSet(copy1))
            processor.valueRemoved(pointer.append(field), source.get(field));

        final Set<String> copy2 = new HashSet<String>(secondFields);
        copy2.removeAll(firstFields);


        for (final String field : Collections.unmodifiableSet(copy2))
            processor.valueAdded(pointer.append(field), target.get(field));

        final Set<String> intersection = new HashSet<String>(firstFields);
        intersection.retainAll(secondFields);

        for (final String field : intersection)
            generateDiffs(processor, pointer.append(field), source.get(field),
                    target.get(field));
    }

    private static <T> Set<T> collect(Iterator<T> from, Set<T> to) {
        if (from == null) {
            throw new NullPointerException();
        }
        if (to == null) {
            throw new NullPointerException();
        }
        while (from.hasNext()) {
            to.add(from.next());
        }
        return Collections.unmodifiableSet(to);
    }


    private static void generateArrayDiffs(final DiffProcessor processor,
                                           final JsonPointerCustom pointer, final ArrayNode source,
                                           final ArrayNode target) {
        final int firstSize = source.size();
        final int secondSize = target.size();
        final int size = Math.min(firstSize, secondSize);

        /*
         * Source array is larger; in this case, elements are removed from the
         * target; the index of removal is always the original arrays's length.
         */
        for (int index = size; index < firstSize; index++)
            processor.valueRemoved(pointer.append(size), source.get(index));

        for (int index = 0; index < size; index++)
            generateDiffs(processor, pointer.append(index), source.get(index),
                    target.get(index));

        // Deal with the destination array being larger...
        for (int index = size; index < secondSize; index++)
            processor.valueAdded(pointer.append("-"), target.get(index));
    }


    static Map<JsonPointerCustom, JsonNode> getUnchangedValues(final JsonNode source,
                                                               final JsonNode target) {
        final Map<JsonPointerCustom, JsonNode> ret = new HashMap<JsonPointerCustom, JsonNode>();
        computeUnchanged(ret, JsonPointerCustom.empty(), source, target);
        return ret;
    }

    private static void computeUnchanged(final Map<JsonPointerCustom, JsonNode> ret,
                                         final JsonPointerCustom pointer, final JsonNode first, final JsonNode second) {
        //if first and second are equal
        if (EQUIVALENCE.equivalent(first, second)) {
            ret.put(pointer, second);
            return;
        }

        final NodeType firstType = NodeType.getNodeType(first);
        final NodeType secondType = NodeType.getNodeType(second);

        if (firstType != secondType)
            return; // nothing in common

        // We know they are both the same type, so...
        switch (firstType) {
            case OBJECT:
                computeObject(ret, pointer, first, second);
                break;
            case ARRAY:
                computeArray(ret, pointer, first, second);
                break;
            default:
                /* nothing */
        }
    }

    private static void computeObject(final Map<JsonPointerCustom, JsonNode> ret,
                                      final JsonPointerCustom pointer, final JsonNode source,
                                      final JsonNode target) {
        final Iterator<String> firstFields = source.fieldNames();

        String name;

        while (firstFields.hasNext()) {
            name = firstFields.next();
            if (!target.has(name))
                continue;
            computeUnchanged(ret, pointer.append(name), source.get(name),
                    target.get(name));
        }
    }

    private static void computeArray(final Map<JsonPointerCustom, JsonNode> ret,
                                     final JsonPointerCustom pointer, final JsonNode source, final JsonNode target) {
        final int size = Math.min(source.size(), target.size());

        for (int i = 0; i < size; i++)
            computeUnchanged(ret, pointer.append(i), source.get(i),
                    target.get(i));
    }
}
