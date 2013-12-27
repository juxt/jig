(require '(clojure [string :refer (join)]
                   [edn :as edn])
         '(clojure.java [shell :refer (sh)]
                        [io :as io]))

(def default-version "0.0.1-SNAPSHOT")

(defn git [git-dir & args]
  (apply sh (concat ["git" (format "--git-dir=%s" (.getAbsolutePath git-dir))] args)))

(defn head-ok [git-dir]
  (-> (git git-dir "rev-parse" "--verify" "HEAD")
      :exit zero?))

(defn refresh-index [git-dir]
  (git git-dir "update-index" "-q" "--ignore-submodules" "--refresh"))

(defn unstaged-changes [git-dir]
  (-> (git git-dir "diff-files" "--quiet" "--ignore-submodules")
      :exit zero? not))

(defn uncommitted-changes [git-dir]
  (-> (git git-dir "diff-index" "--cached" "--quiet" "--ignore-submodules" "HEAD" "--")
      :exit zero? not))

(defn get-git-dir [f]
  (loop [dir (io/file f)]
    (let [git-dir (io/file dir ".git")]
      (if (.exists git-dir) git-dir
          (when (.getParentFile dir)
            (recur (.getParentFile dir)))))))

(defn bump-version [tag]
  (let [[[_ stem lst]] (re-seq #"(.*\..*)(\d+)" tag)]
    (join [stem (inc (read-string lst)) "-" "SNAPSHOT"])))

;; We don't want to keep having to 'bump' the version when we are
;; sitting on a more capable versioning system: git.
(defn get-version []
  (let [git-dir (get-git-dir (System/getProperty "user.dir"))]
    (cond
     (nil? git-dir) default-version

     (not (head-ok git-dir)) (throw (ex-info "HEAD not valid" {}))

     :otherwise
     (do
       (refresh-index git-dir)
       (let [{:keys [exit out err]} (sh "git" "describe" "--tags" "--long")]
         (if (= 128 exit) default-version
             (let [[[_ tag commits hash]] (re-seq #"(.*)-(.*)-(.*)" out)]
               (if (and
                    (zero? (edn/read-string commits))
                    (not (unstaged-changes git-dir))
                    (not (uncommitted-changes git-dir)))
                 tag
                 (bump-version tag)
                 ))))))))
