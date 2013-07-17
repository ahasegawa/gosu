package gw.plugin.ij.util.transform.java;


import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import gw.plugin.ij.util.transform.java.Visitor.GosuVisitor;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class GosuVisitorTestUtil {

  public String transform(String jsrc) throws IOException {
    String src = readFile(jsrc);
    if (src == null) {
      return null;
    }

    JavaCompiler compiler = JavacTool.create();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
    ArrayList<JavaStringObject> javaStringObjects = new ArrayList<JavaStringObject>();
    javaStringObjects.add(new JavaStringObject(src));
    Iterable<? extends JavaFileObject> compilationUnits = javaStringObjects;
    StringWriter errors = new StringWriter();
    JavaCompiler.CompilationTask task = compiler.getTask(errors, fileManager, null, null, null, compilationUnits);
    JavacTaskImpl javacTask = (JavacTaskImpl) task;
    String output = "";
    try {
      Iterable<? extends CompilationUnitTree> trees = javacTask.parse();
      if (errors.toString().isEmpty()) {
        GosuVisitor visitor = new GosuVisitor(4);
        for (CompilationUnitTree tree : trees) {
          tree.accept(visitor, null);
        }
        fileManager.close();
        output = visitor.getOutput().toString();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return output;
  }

  public String readFile(String path) throws FileNotFoundException {
    InputStream in = getClass().getClassLoader().getResourceAsStream(path);
    StringBuilder src = new StringBuilder();
    Scanner scanner = null;
    try {
      scanner = new Scanner(in, "UTF-8");

      while (scanner.hasNextLine()) {
        src.append(scanner.nextLine() + "\n");
      }
    } finally {
      scanner.close();
    }
    return src.toString();
  }
}
