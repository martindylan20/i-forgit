import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Git {
    public static void main(String[] args) throws IOException {
        Git.init();
        // System.out.println(Git.hashFile("testFolder/test2.txt"));
        Git.createBlob("testFolder/test2.txt");
        Git.updateIndex("testFolder/test2.txt");

        Git.createBlob("test.txt");
        Git.updateIndex("test.txt");
        treeIndex();
    }

    public static void init() {
        File gitDir = new File("./git/");
        if (!gitDir.mkdir()) {
            System.out.println("Git Repository Already Exists");
        }

        File objDir = new File("./git/objects/");
        File index = new File("./git/index");
        File head = new File("./git/HEAD");

        try {
            if (!objDir.mkdir() || !index.createNewFile() || !head.createNewFile()) {
                System.out.println("Git Repository Already Exists");
                return;
            }
        } catch (IOException e) {
            System.out.println("Failed to initialize Git repo");
            return;
        }

        System.out.println("Git Repository Created");

    }

    public static String hashFile(String filePath) throws IOException {
        StringBuilder stuffInFile = new StringBuilder();

        // gets stuffInFile
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                stuffInFile.append(line);
            }
        }

        // turn stuffInFile to bytes
        
        return hashString(stuffInFile.toString());

    }

    private static String hashString(String input) {
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.toString().getBytes(StandardCharsets.UTF_8));

            // bytes to hash
            for (byte b : bytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            System.out.println("NoSuchAlgorithmException: file couldn't be hashed :(");
        }
        return null;
    }

    public static void createBlob(String filePath) throws IOException {
        // get hash of the file
        String hashedFile = Git.hashFile(filePath);

        // write original file content into the obj
        Path sourcePath = Paths.get(filePath);
        Path destinationPath = Paths.get("./git/objects/" + hashedFile);

        try {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully");
        }

        catch (IOException e) {
            System.out.println("Error during file copying: " + e);
        }

    }

    public static void updateIndex(String filePath) throws IOException {
        String hashedFile = Git.hashFile(filePath);
        File index = new File("./git/index");
        String root = new File("..").getAbsoluteFile().getParentFile().getName();

        // getting path of filePath
        Path path = Paths.get(filePath);
        String relativePath = path.toString();
        String entry =  hashedFile + " " + root + "/" + relativePath;

        // checking if needs newLine or index is empty
        ArrayList<String> lines = new ArrayList<>();
        boolean updated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(index))) {
            String line = br.readLine();

            // reading the existing index file (going line by line)
            while (line != null) {
                // add same filePath, but with diff hash
                if (samePath(line, root + "/" + relativePath)) {
                    lines.add(entry);
                    updated = true;
                }

                // add existing line (different filePath)
                else {
                    lines.add(line);
                }
                line = br.readLine();
            }
        }

        // if filePath doesn't exist yet
        if (!updated) {
            lines.add(entry);
        }

        // rewrite index with the updated stuff
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(index))) {
            int count = 0;
            for (String line : lines) {
                bw.write(line);

                // making sure no extra new line at the end
                if (count < lines.size() - 1) {
                    bw.newLine();
                }

                count++;
            }
        }

        catch (Exception e) {
            System.out.println("Failed to update ./git/index: " + e);
        }

    }

    // helper method to check whether a line in index file has the same file path
    public static boolean samePath(String line, String filePath) {
        int spaceInd = line.indexOf(" ");

        if (spaceInd == -1) {
            return false;
        }

        String existingPath = line.substring(spaceInd + 1);
        return existingPath.equals(filePath);
    }

    public static String createTree(ArrayList<String> workingList, String dirPath)
            throws IOException {
        StringBuilder treeContents = new StringBuilder();
        boolean firstLine = true;

        for (String line : workingList) {
            String[] parts = line.split(" ", 3);

            String type = parts[0];
            String hash = parts[1];
            String path = parts[2];

            int lastSlash = path.lastIndexOf("/");

            if (lastSlash != -1) {
                String folder = path.substring(0, lastSlash);

                if (folder.equals(dirPath)) {
                    String name = path.substring(lastSlash + 1);

                    if (!firstLine) {
                        treeContents.append("\n");
                    }

                    treeContents.append(type + " " + hash + " " + name);
                    firstLine = false;
                }
            }
        }
        String contents = treeContents.toString();

        String hashed = hashString(contents);

        File treeFile = new File("git/objects", hashed);

        try {
            treeFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(treeFile))) {
            writer.write(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hashed;
    }

    // unfortunately I am sick and have only been able to do 3.2
    public static String treeIndex() {
        try {
            File indexFile = new File("./git/index"); 
            StringBuilder fileString = new StringBuilder();
            BufferedReader brFile = new BufferedReader(new FileReader(indexFile));
            String line = "";
            while ((line = brFile.readLine()) != null) {
                fileString.append(line);
                fileString.append('\n');
            }
            brFile.close();
            ArrayList<String> indexArray = new ArrayList<>(Arrays.asList(fileString.substring(0,fileString.length()-1).split("\n")));
            for (int i = 0; i < indexArray.size(); i++) {
                indexArray.set(i, "blob " + indexArray.get(i));
            }
            // String index = "";
            String deepPath = "/";
            String root = new File("..").getAbsoluteFile().getParentFile().getName();
            
            while (!deepPath.equals(root) && deepPath.length()!=0) {
                deepPath = "";
                for (String string : indexArray) {
                    if ((deepPath.length() - deepPath.replace("/", "").length()) < (string.length() - string.replace("/", "").length())) {
                        deepPath = new File(string.substring(string.indexOf(' ', string.indexOf(' ') + 1) + 1)).getParent();
                    }
                }
                ArrayList<Integer> remove = new ArrayList<>();
                for (int i = 0; i < indexArray.size(); i++) {
                    if (indexArray.get(i).contains(deepPath)) {
                        remove.add(i);
                    }
                }
                indexArray.add("tree " + createTree(indexArray, deepPath) + " " + deepPath);
                for (int i = 0; i < remove.size(); i++) {
                    indexArray.remove((int) remove.get(i)-i);
                }
            }
            FileWriter fw = new FileWriter("./git/index");
            fw.write(indexArray.get(0));
            fw.close();
            return createTree(indexArray, deepPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

