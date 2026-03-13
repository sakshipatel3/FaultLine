#!/usr/bin/env bash
# Initializes a demo git repository with a few Java files and commits for analysis.
set -euo pipefail

REPO_DIR="$(pwd)"
if [ -d ".git" ]; then
  echo "Repository already initialized in $(pwd)"
  exit 0
fi

git init

cat > src/main/java/com/example/demo/App.java <<'JAVA'
package com.example.demo;

public class App {
    public static void main(String[] args) {
        System.out.println("Hello Demo");
    }
}
JAVA

mkdir -p src/main/java/com/example/demo/utils
cat > src/main/java/com/example/demo/utils/Util.java <<'JAVA'
package com.example.demo.utils;

public class Util {
    public static int add(int a, int b) {
        return a + b;
    }

    public static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<n;i++) sb.append(s);
        return sb.toString();
    }
}
JAVA

git add .
git commit -m "Initial commit with demo java files"

# Make a few changes to create churn and multiple commits
sed -i 's/Hello Demo/Hello Demo v1/' src/main/java/com/example/demo/App.java
git add .
git commit -m "Update greeting"

sed -i 's/return a + b;/return a + b + 1;/' src/main/java/com/example/demo/utils/Util.java
git add .
git commit -m "Change util add behavior"

# Add another class with a long method to simulate complexity
cat > src/main/java/com/example/demo/Complex.java <<'JAVA'
package com.example.demo;

public class Complex {
    public void longMethod() {
        int a = 0;
        for (int i=0;i<200;i++) {
            a += i;
        }
        // many lines to simulate long method
        for (int j=0;j<200;j++) {
            a += j * 2;
        }
    }
}
JAVA

git add .
git commit -m "Add Complex class with long method"

echo "Demo repository initialized at $(pwd)"
