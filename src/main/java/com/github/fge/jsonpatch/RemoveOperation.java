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


//        this.type = type;
//        this.from = from;
//        this.oldValue = oldValue;
//        this.path = path;
//        this.value = value;

package com.github.fge.jsonpatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointerCustom;
import com.github.fge.jackson.jsonpointer.JsonPointerException;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON Path {@code remove} operation
 *
 * <p>This operation only takes one pointer ({@code path}) as an argument. It
 * is an error condition if no JSON value exists at that pointer.</p>
 */
public final class RemoveOperation extends JsonPatchOperation {

//    Logger logger = LoggerFactory.getLogger(RemoveOperation.class);

    @JsonCreator
    public RemoveOperation(@JsonProperty("path") final JsonPointerCustom path,@JsonProperty("originalValue") final JsonNode originalValue) {
        super("remove", path,originalValue);
    }

    private int getNodeToRemove(JsonNode valueLocatorNode, ArrayNode array) {

        if (array == null) return -1;

        for (int i = 0; i < array.size(); i++) {
            JsonNode currentNode = array.get(i);
            ObjectMapper mapper = new ObjectMapper();
            //convert valueLocatorNode to map
            Map<String, Object> valueLocatorMap = mapper.convertValue(valueLocatorNode, new TypeReference<Map<String, Object>>() {
            });
            //make flag true
            boolean flag = true;
            //check if current node has value that are equal to value locator node
            for (Map.Entry<String, Object> entry : valueLocatorMap.entrySet()) {
                String currentKey = entry.getKey();
                //if keys are present
                if (!valueLocatorNode.get(currentKey).equals(currentNode.get(currentKey))) {
                    flag = false;
                    break;
                }
            }
            //if all values that are in the value locator node are present then return the index
            if (flag) return i;
        }
        //if node or path not found
        return -1;
    }

    @Override
    public JsonNode apply(final JsonNode node) throws JsonPatchException {
        if (path.isEmpty()) return MissingNode.getInstance();
        if (path.path(node).isMissingNode()) throw new JsonPatchException(BUNDLE.getMessage("jsonPatch.noSuchPath"));
        final JsonNode ret = node.deepCopy();
        final JsonNode parentNode = path.parent().get(ret);
        final String raw = Iterables.getLast(path).getToken().getRaw();
        if (parentNode.isObject()) ((ObjectNode) parentNode).remove(raw);
        else ((ArrayNode) parentNode).remove(Integer.parseInt(raw));
        return ret;
    }

    private void applyStrictValidation(boolean flag) throws JsonPatchException {
        if (flag) {
            throw new JsonPatchException(BUNDLE.getMessage("jsonPatch.noSuchPath"));
        } else {
           // logger.error("jsonPatch.noSuchPath");
        }
    }

    private static ObjectNode getValueLocator(JsonNode ogValue) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> valueLocatorMap = mapper.convertValue(ogValue, new TypeReference<Map<String, Object>>() {
        });
        ObjectNode valueLocator = new ObjectMapper().createObjectNode();
        Iterator<Map.Entry<String, Object>> iterator = valueLocatorMap.entrySet().iterator();
        int i = 0;
        while (i < 3) {
            Map.Entry<String, Object> entry = iterator.next();
            String currentKey = entry.getKey();
            i++;
        }
        return null;
    }

    @Override
    public JsonNode apply(JsonNode node, boolean flag) throws JsonPatchException {
//        if (path == null || value_locator == null) {
//            applyStrictValidation(flag);
//            return node;
//        }
//        JsonNode result;
//        //see if path is null and check if its has ?
//        if (path.toString().contains("?")) {
//            //get the value locator
//            JsonNode valueLocatorNode = value_locator.deepCopy();
//            JsonPointerCustom arrayNodePath = null;
//            try {
//                //get the path before ? i.e.>> array node
//                arrayNodePath = JsonPointerCustom.getBeforeUnknown(path.toString());
//            } catch (JsonPointerException e) {
//                applyStrictValidation(flag);
//            }
//            String raw = null;
//            //get the raw representation of the field i.e >> array node field
//            if (arrayNodePath != null) {
//                raw = Iterables.getLast(arrayNodePath).getToken().getRaw();
//                //get the array node
//                ArrayNode arrayNode = (ArrayNode) node.get(raw);
//                if (arrayNode == null) {
//                    applyStrictValidation(flag);
//                    return node;
//                }
//                //taking index of node that we want to remove using valueLocatorNode
//                int toRemove = getNodeToRemove(valueLocatorNode, arrayNode);
//                // if its not present then throw exception
//                if (toRemove == -1) applyStrictValidation(flag);
//                //remove the node
//                arrayNode.remove(toRemove);
//
//            }
//            result = node;
//        } else {
//            result = apply(node);
//        }
        return null;
    }

    @Override
    public void serialize(final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
        jgen.writeStartObject();
        jgen.writeStringField("op", "remove");
        jgen.writeStringField("path", path.toString());
        jgen.writeEndObject();
    }

    @Override
    public void serializeWithType(final JsonGenerator jgen, final SerializerProvider provider, final TypeSerializer typeSer) throws IOException {
        serialize(jgen, provider);
    }

    @Override
    public String toString() {
        return "op: " + op + "; path: \"" + path + '"' + "; originalValue: \"" + originalValue + '"';

    }


}
