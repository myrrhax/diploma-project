package com.github.myrrhax.diploma_project.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SchemaHashGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private static final Set<String> IGNORED_KEYS = Set.of(
            "id", "schemaId", "tableId", "x", "y", "cacheVersion",
            "lastModificationTime", "schemaState", "lock", "linkedColumns",
            "description", "primaryKeyParts", "autoIncrementedColumn"
    );

    public static String hashSchema(String originalJson) throws Exception {
        JsonNode root = MAPPER.readTree(originalJson);
        cleanAndTransformTree(root);
        String canonicalJson = MAPPER.writeValueAsString(root);

        return calculateMd5(canonicalJson);
    }

    private static void cleanAndTransformTree(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.remove(IGNORED_KEYS);

            transformMapToArray(objectNode, "tables", "name");
            transformMapToArray(objectNode, "columns", "name");
            transformMapToArray(objectNode, "indexes", "indexName");
            transformMapToArray(objectNode, "references", "key");

            Iterator<JsonNode> elements = objectNode.elements();
            while (elements.hasNext()) {
                cleanAndTransformTree(elements.next());
            }

        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;

            for (JsonNode element : arrayNode) {
                cleanAndTransformTree(element);
            }

            sortArrayNode(arrayNode);
        }
    }

    private static void transformMapToArray(ObjectNode parent, String mapFieldName, String sortByField) {
        JsonNode mapNode = parent.get(mapFieldName);

        if (mapNode != null && mapNode.isObject()) {
            List<JsonNode> valuesList = new ArrayList<>();
            Iterator<JsonNode> values = mapNode.elements();
            while (values.hasNext()) {
                valuesList.add(values.next());
            }

            valuesList.sort((a, b) -> {
                JsonNode aVal = a.get(sortByField);
                JsonNode bVal = b.get(sortByField);

                String strA = (aVal != null && !aVal.isNull()) ? aVal.asText() : "";
                String strB = (bVal != null && !bVal.isNull()) ? bVal.asText() : "";

                if (strA.equals(strB)) {
                    return a.toString().compareTo(b.toString());
                }
                return strA.compareTo(strB);
            });

            ArrayNode newArrayNode = MAPPER.createArrayNode();
            newArrayNode.addAll(valuesList);

            parent.set(mapFieldName, newArrayNode);
        }
    }

    private static void sortArrayNode(ArrayNode arrayNode) {
        List<JsonNode> elements = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            elements.add(node);
        }
        elements.sort(Comparator.comparing(JsonNode::toString));

        arrayNode.removeAll();
        arrayNode.addAll(elements);
    }

    private static String calculateMd5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}