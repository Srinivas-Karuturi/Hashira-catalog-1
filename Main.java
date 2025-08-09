import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class Main {
    
    public static void main(String[] args) {
        try {
            // Check if file paths are provided as command line arguments
            if (args.length > 0) {
                // Read from files provided as command line arguments
                for (int i = 0; i < args.length; i++) {
                    System.out.println("=== Test Case " + (i + 1) + " (from " + args[i] + ") ===");
                    BigInteger secret = solveFromJsonFile(args[i]);
                    System.out.println("Secret (c): " + secret);
                    System.out.println();
                }
            } else {
                // Default behavior - look for specific files
                String[] testFiles = {"testcase1.json", "testcase2.json"};
                
                for (int i = 0; i < testFiles.length; i++) {
                    File file = new File(testFiles[i]);
                    if (file.exists()) {
                        System.out.println("=== Test Case " + (i + 1) + " (from " + testFiles[i] + ") ===");
                        BigInteger secret = solveFromJsonFile(testFiles[i]);
                        System.out.println("Secret (c): " + secret);
                        System.out.println();
                    } else {
                        System.out.println("File " + testFiles[i] + " not found, skipping...");
                    }
                }
                
                // If no files found, show usage
                if (!new File("testcase1.json").exists() && !new File("testcase2.json").exists()) {
                    System.out.println("Usage:");
                    System.out.println("1. Create JSON files named 'testcase1.json' and 'testcase2.json' in the current directory");
                    System.out.println("2. Or run: java Main <file1.json> <file2.json> ...");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Solves the secret sharing problem from a JSON string
     */
    public static BigInteger solveFromJsonString(String jsonString) throws Exception {
        Map<String, Object> jsonData = parseJsonString(jsonString);
        return solve(jsonData);
    }
    
    /**
     * Solves the secret sharing problem from a JSON file
     */
    public static BigInteger solveFromJsonFile(String filePath) throws Exception {
        String jsonContent = readFileAsString(filePath);
        return solveFromJsonString(jsonContent);
    }
    
    /**
     * Simple JSON parser for our specific use case
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJsonString(String json) {
        Map<String, Object> result = new HashMap<>();
        
        // Remove whitespace and braces
        json = json.trim().replaceAll("\\s+", "");
        json = json.substring(1, json.length() - 1); // Remove outer braces
        
        // Split by comma, but be careful about nested objects
        List<String> keyValuePairs = splitByComma(json);
        
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split(":", 2);
            String key = keyValue[0].replaceAll("\"", "").trim();
            String value = keyValue[1].trim();
            
            if (value.startsWith("{") && value.endsWith("}")) {
                // It's a nested object
                result.put(key, parseJsonString(value));
            } else {
                // It's a simple value
                value = value.replaceAll("\"", "");
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * Helper method to split JSON by comma while respecting nested structures
     */
    private static List<String> splitByComma(String json) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceLevel = 0;
        boolean inQuotes = false;
        
        for (char c : json.toCharArray()) {
            if (c == '"' && (current.length() == 0 || current.charAt(current.length() - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            
            if (!inQuotes) {
                if (c == '{') {
                    braceLevel++;
                } else if (c == '}') {
                    braceLevel--;
                }
            }
            
            if (c == ',' && braceLevel == 0 && !inQuotes) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        
        return parts;
    }
    
    /**
     * Reads file content as string
     */
    private static String readFileAsString(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    /**
     * Main solving logic
     */
    @SuppressWarnings("unchecked")
    public static BigInteger solve(Map<String, Object> data) throws Exception {
        // Parse keys
        Map<String, Object> keys = (Map<String, Object>) data.get("keys");
        int n = Integer.parseInt((String) keys.get("n"));
        int k = Integer.parseInt((String) keys.get("k"));
        
        // Parse and decode all points
        List<Point> points = new ArrayList<>();
        
        for (String key : data.keySet()) {
            // Skip the "keys" field
            if (key.equals("keys")) {
                continue;
            }
            
            Map<String, Object> pointData = (Map<String, Object>) data.get(key);
            int x = Integer.parseInt(key);
            int base = Integer.parseInt((String) pointData.get("base"));
            String encodedValue = (String) pointData.get("value");
            
            // Decode the y value from the given base
            BigInteger y = decodeValue(encodedValue, base);
            
            points.add(new Point(BigInteger.valueOf(x), y));
        }
        
        // Sort points by x coordinate for consistency
        points.sort((p1, p2) -> p1.x.compareTo(p2.x));
        
        // Use only the first k points for Lagrange interpolation
        List<Point> selectedPoints = points.subList(0, Math.min(k, points.size()));
        
        // Find the secret using Lagrange interpolation (evaluate polynomial at x=0)
        BigInteger secret = lagrangeInterpolation(selectedPoints, BigInteger.ZERO);
        
        return secret;
    }
    
    /**
     * Decodes a value from the given base to decimal
     */
    public static BigInteger decodeValue(String value, int base) {
        return new BigInteger(value, base);
    }
    
    /**
     * Performs Lagrange interpolation to find the polynomial value at x=target
     * For secret sharing, we evaluate at x=0 to find the constant term (secret)
     */
    public static BigInteger lagrangeInterpolation(List<Point> points, BigInteger target) {
        BigInteger result = BigInteger.ZERO;
        
        for (int i = 0; i < points.size(); i++) {
            Point pi = points.get(i);
            BigInteger term = pi.y;
            
            // Calculate the Lagrange basis polynomial Li(target)
            for (int j = 0; j < points.size(); j++) {
                if (i != j) {
                    Point pj = points.get(j);
                    
                    // Li(target) *= (target - xj) / (xi - xj)
                    BigInteger numerator = target.subtract(pj.x);
                    BigInteger denominator = pi.x.subtract(pj.x);
                    
                    // For integer arithmetic, we need to be careful with division
                    // In this case, we know the result should be an integer
                    term = term.multiply(numerator).divide(denominator);
                }
            }
            
            result = result.add(term);
        }
        
        return result;
    }
    
    /**
     * Point class to represent (x, y) coordinates
     */
    static class Point {
        BigInteger x;
        BigInteger y;
        
        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }
}
