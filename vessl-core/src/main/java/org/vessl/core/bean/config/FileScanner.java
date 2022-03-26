package org.vessl.core.bean.config;

import org.apache.commons.lang3.StringUtils;
import org.vessl.core.spi.FileScanPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * 扫描文件 并返回inputstream供读取
 *
 * @author ljb
 */
public class FileScanner {


    private final String SCAN_DEEP_CLASSPATH_FLAG = "classpath*:";
    private final String SCAN_CLASSPATH_FLAG = "classpath:";
    private final String SCAN_LOCAL_FLAG = "file:";

    /**
     * 防止重复读取文件
     */
    Set<String> files = new HashSet<>();
    /**
     * 读取到的文件流
     */
    List<InputStream> inputStreams = new ArrayList<>();


    public void scan(FileScanPlugin scanHandler, String path) {
        if (StringUtils.isNotEmpty(path)) {
            scan(path);
            scanHandler.handle(inputStreams);
            for (InputStream inputStream : inputStreams) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {

                }
            }
            files.clear();
            inputStreams.clear();
        }
    }


    private void scan(String paths) {

        String[] split = paths.split(",");
        for (String path : split) {

            boolean scanDeep = false;
            boolean isClasspath = false;
            if (path.startsWith(SCAN_DEEP_CLASSPATH_FLAG)) {
                scanDeep = true;
                isClasspath = true;
                path = path.replace(SCAN_DEEP_CLASSPATH_FLAG, "");
            } else if (path.startsWith(SCAN_CLASSPATH_FLAG)) {
                isClasspath = true;
                path = path.replace(SCAN_CLASSPATH_FLAG, "");

            } else if (path.startsWith(SCAN_LOCAL_FLAG)) {
                path = path.replace(SCAN_LOCAL_FLAG, "");
            }
            if (StringUtils.isEmpty(path) || path.startsWith("/") || path.startsWith("*")) {
                //TODO warn
                continue;
            }

            String regexPath = getRegexPath(path);
            Pattern pattern = null;
            if (StringUtils.isNotEmpty(regexPath)) {
                pattern = Pattern.compile(regexPath);
            }

            scanFile(path, pattern, isClasspath, scanDeep);
        }

    }

    private String getRegexPath(String path) {
        if (path.contains("*")) {
            return path.replace(".", "\\.").replace("**", ".|").replace("*", "\\w*").replace(".|", ".*");
        }
        return "";
    }


    /**
     * @param path           从该路径开始扫描
     * @param regexPath      匹配正则路径,如果为空,则表示为精准匹配 startPath即为绝对匹配
     * @param mapperScanDeep 是否扫描所有jar包
     */
    public void scanFile(String path, Pattern regexPath, boolean isClasspath, boolean mapperScanDeep) {

        if (isClasspath) {

            String startPath;
            if (path.contains("*")) {
                startPath = path.substring(0, path.lastIndexOf("/", path.indexOf("*")));
            } else {
                startPath = path;
            }
            ArrayList<URL> list = new ArrayList<>();
            try {
                list = Collections.list(Thread.currentThread().getContextClassLoader().getResources(startPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (URL url : list) {
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    // 引用jar包的文件
                    findLocalFile(url.getPath(), path, regexPath);
                } else if ("jar".equals(protocol)) {
                    // 引用jar包的文件
                    findFileJar(url, startPath, path, regexPath);
                }
                if (!mapperScanDeep) {
                    return;
                }
            }

        } else {
            //即使扫描的不是classpath,如果是相对路径则还是扫描classpath
            findLocalFile(path, regexPath);
        }

    }

    private void findLocalFile(String path, Pattern regexPath) {
        String scanPath;

        if (regexPath != null) {
            //如果是通配路径,获取开始扫描的路径
            String startPath = path.substring(0, path.lastIndexOf("/", path.indexOf("*")));
            //如果不是绝对路径,则补充为绝对路径
            if (isAbsPath(startPath)) {
                scanPath = startPath;
            } else {
                scanPath = currentPath() + startPath;
            }
            findLocalFile(scanPath, startPath, regexPath);
        } else {
            //精确路径不扫描,直接读取

            //如果不是绝对路径,则补充为绝对路径
            if (isAbsPath(path)) {
                scanPath = path;
            } else {
                scanPath = currentPath() + path;
            }
            if (!files.contains(scanPath)) {
                Path path1 = Paths.get(scanPath);
                if (Files.exists(path1)) {
                    try {

                        InputStream inputStream = Files.newInputStream(path1);
                        inputStreams.add(inputStream);
                        files.add(scanPath);

                    } catch (IOException e) {
                        //TODO 异常
                    }
                }
            }
        }
    }


    /**
     * 本地查找
     *
     * @param absPath      当前扫描绝对路径
     * @param relativePath 当前扫描相对路径
     * @param matchRegPath 正则匹配
     */
    private void findLocalFile(String absPath, String relativePath, Pattern matchRegPath) {

        File file = new File(absPath);
        //如果是文件表示已经匹配
        if (file.isFile()) {
            String fileAbsPath = file.getAbsolutePath().replace("\\", "/");
            if (!files.contains(fileAbsPath)) {
                try {
                    InputStream inputStream = Files.newInputStream(Paths.get(fileAbsPath));
                    inputStreams.add(inputStream);
                    files.add(fileAbsPath);
                } catch (IOException e) {
                    //TODO 异常
                }

            }
            return;
        }
        File[] files1 = file.listFiles();
        for (File chiFile : files1) {
            String fileAbsPath = chiFile.getAbsolutePath().replace("\\", "/");
            String fileRelativePath = relativePath + "/" + chiFile.getName();
            if (chiFile.isDirectory()) {
                findLocalFile(fileAbsPath, fileRelativePath, matchRegPath);
            }
            if (matchRegPath.matcher(fileRelativePath).matches()) {
                if (!files.contains(fileAbsPath)) {
                    try {
                        InputStream inputStream = Files.newInputStream(Paths.get(fileAbsPath));
                        inputStreams.add(inputStream);
                        files.add(fileAbsPath);
                    } catch (IOException e) {
                        //TODO 异常
                    }


                }
            }

        }


    }


    /**
     * jar包查找
     *
     */
    private void findFileJar(URL url, final String packName, String absPath, Pattern regPath) {
        String pathName = packName.replace(".", "/");
        JarFile jarFile = null;
        try {

            JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
            jarFile = jarURLConnection.getJarFile();
        } catch (IOException e) {
            throw new RuntimeException("未找到策略资源");
        }

        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String jarEntryName = jarEntry.getName();

            if (jarEntryName.contains(pathName) && !jarEntryName.equals(pathName + "/")) {

                if (regPath == null && jarEntryName.equals(absPath) || regPath.matcher(jarEntryName).matches()) {
                    String key = url + "/" + jarEntryName;
                    if (!files.contains(key)) {
                        try {
                            InputStream inputStream = jarFile.getInputStream(jarEntry);
                            files.add(key);
                            inputStreams.add(inputStream);
                        } catch (IOException e) {
                            //TODO WARN
                        }
                    }
                }

            }

        }
    }

    private boolean isWin() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("win");
    }

    private boolean isAbsPath(String path) {
        if (isWin()) {
            return path.indexOf(":") == 1;
        }
        return path.startsWith("/");
    }

    private boolean isRunInJar() {
        String basePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        return basePath.toLowerCase().endsWith(".jar");
    }

    private String currentPath() {
        String basePath = "";
        if (!isRunInJar()) {
            basePath = this.getClass().getClassLoader().getResource("").getPath();
        } else {
            basePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            if (basePath.toLowerCase().endsWith(".jar")) {
                basePath = basePath.substring(0, basePath.lastIndexOf("/"));
            }
        }
        if (isWin()) {
            basePath = basePath.substring(1);
        }
        return basePath;
    }
}