(ns core
  (:require [babashka.cli :as cli]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b])
  (:import (java.io File)))

(defn java-source-path [group-id artifact-id]
  (str (str/replace group-id "." "/") "/" artifact-id))

(defn replace-hyphen [^String x]
  (str/replace x "-" "_"))

(def lib 'myapp/myapp)
(def group-id (namespace lib))
(def artifact-id (name lib))
(def root-package (replace-hyphen artifact-id))
;; For dynamic versioning from git, use:
;;   (def version (format "1.2.%s" (b/git-count-revs nil)))
(def version "0.1.0")
(def clj-source (str "src/clj/" root-package))
(def java-source (str "src/java/" (java-source-path group-id root-package)))
(def resources "src/resources")
(def test-clj-source (str "test/clj/" root-package))
(def test-java-source (str "test/java/" (java-source-path group-id root-package)))
(def test-resources "test/resources")
(def target-dir "target")
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" artifact-id version))
(def uber-file (format "target/%s-%s-standalone.jar" artifact-id version))
(def native-image-file (format "target/native/%s" artifact-id))
(def main-class (str artifact-id ".core"))

(defn java-file-exist? [dir]
  (let [files (file-seq (io/file dir))]
    (some #(str/ends-with? (.getName %) ".java")
          (filter #(.isFile %) files))))

(defn gen-basis
  "Returns the basis from opts if already resolved, otherwise creates one."
  [opts]
  (or (:basis opts)
      (b/create-basis {:project "deps.edn"
                       :aliases (:aliases opts)})))

(defn init
  "init project structure, create necessary directories"
  [_]
  (println "Initializing...")
  (let [source-dirs [clj-source java-source resources]
        test-dirs   [test-clj-source test-java-source test-resources]
        all-dirs    (concat source-dirs test-dirs)]
    (doseq [^File f (map io/file all-dirs)]
      (when-not (.exists f)
        (.mkdirs f)))))

(defn clean
  "Delete the build target directory"
  [_]
  (println "Cleanup...")
  (b/delete {:path target-dir}))

(defn prep
  "prepare for building"
  [opts]
  (init opts)
  (println "Writing pom.xml...")
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     (gen-basis opts)
                :src-dirs  [java-source clj-source]})
  (println "Copying resources...")
  (b/copy-dir {:src-dirs   [resources]
               :target-dir class-dir}))

(defn compile-java
  "compile java source files"
  [opts]
  (println "Compiling java sources...")
  (when (java-file-exist? java-source)
    (b/javac {:src-dirs   [java-source]
              :class-dir  class-dir
              :basis      (gen-basis opts)
              :javac-opts ["-source" "8" "-target" "8"]})))

(defn compile-clj
  "compile clojure source files"
  [opts]
  (println "Compiling clojure sources...")
  (b/compile-clj {:basis     (gen-basis opts)
                  :src-dirs  [clj-source]
                  :class-dir class-dir}))

(defn compile-all
  "compile all source files"
  [opts]
  (compile-java opts)
  (compile-clj opts))

(defn jar
  "package jar file"
  [opts]
  (let [opts (assoc opts :basis (gen-basis opts))]
    (clean opts)
    (prep opts)
    (compile-all opts)
    (println "Packaging jar...")
    (b/jar {:class-dir class-dir
            :jar-file  jar-file
            :basis     (:basis opts)
            :main      nil})))

(defn uber
  "package uberjar file"
  [opts]
  (let [opts (assoc opts :basis (gen-basis opts))]
    (clean opts)
    (prep opts)
    (compile-all opts)
    (println "Packaging uberjar...")
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis     (:basis opts)
             :main      (or (:main opts) main-class)})))

(defn install-local
  "install jar into local repository"
  [opts]
  (let [opts (assoc opts :basis (gen-basis opts))]
    (jar opts)
    (println "Installing...")
    (b/install {:class-dir class-dir
                :basis     (:basis opts)
                :lib       lib
                :jar-file  jar-file
                :version   version})))

(def spec {:aliases {:alias  :a
                     :coerce [:keyword]
                     :desc   "The aliases used to modify classpath"}
           :main    {:alias :m
                     :desc  "The entrypoint class to run uberjar"}})

(def commands
  {"init"          init
   "clean"         clean
   "prep"          prep
   "compile-java"  compile-java
   "compile-clj"   compile-clj
   "compile-all"   compile-all
   "jar"           jar
   "uber"          uber
   "install-local" install-local})

(defn print-help []
  (println "Usage:")
  (println "  clj -X:build <command> [key val ...]")
  (println "  clj -M:build <command> [arguments...]")
  (println "Supported commands:")
  (doseq [cmd (sort (keys commands))]
    (println (format "  %-14s -- %s" cmd (:doc (meta (commands cmd))))))
  (println "\nSupported Arguments:")
  (println (cli/format-opts {:spec spec :order [:aliases :main]})))

(defn parse-opts [args]
  (cli/parse-opts args {:spec spec}))

(defn -main [& args]
  (if-let [cmd (first args)]
    (if (= cmd "help")
      (print-help)
      (if-let [f (commands cmd)]
        (let [opts (parse-opts (rest args))]
          (println (str "Input options: " opts))
          (f opts))
        (do (println (str "Unknown command: \"" cmd "\""))
            (print-help))))
    (print-help)))
