(defproject org.clojars.pgdad.zoo-leader "1.0.0"
  :description "Leader election using ZooKeeper"
  :warn-on-reflection true
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojars.pgdad/zookeeper-clj "0.9.3"]]
  :aot :all
  :dev-dependencies [[lein-clojars "0.9.0"]]  )