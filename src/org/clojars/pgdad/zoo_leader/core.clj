(ns org.clojars.pgdad.zoo-leader.core
  (:require [org.clojars.pgdad.zookeeper :as zk]
            [org.clojars.pgdad.zookeeper.util :as util]))

(defn node-from-path
  [^String path election-path]
  (.substring path (inc (count election-path))))

(declare elect-leader)

(defn watch-predecessor
  [group-member pred leader {:keys [event-type path]}]
  (if (and (= event-type :NodeDeleted) (= (node-from-path path (:election-path @group-member)) leader))
    ((:leader-notify-fn @group-member) @group-member)
    (if-not (zk/exists (:client @group-member) (str (:election-path @group-member) "/" pred)
                       :watcher (partial watch-predecessor group-member pred leader))
      (elect-leader group-member))))

(defn predecessor
  [me coll]
  (ffirst (filter #(= (second %) me) (partition 2 1 coll))))

(defn elect-leader
  [group-member]
  (let [client (:client @group-member)
        members (util/sort-sequential-nodes
                 (zk/children client (:election-path @group-member)))
        leader (first members)
        me (:me @group-member)
        election-path (:election-path @group-member)]
    (if (= me leader)
      ((:leader-notify-fn @group-member) @group-member)
      (let [pred (predecessor me members)]
        (if-not (zk/exists client (str election-path "/" pred)
                           :watcher (partial watch-predecessor group-member pred leader))
          (elect-leader group-member))))))

(defn join-group
  [keeper election-path leader-notify-fn]
  (let [client (zk/connect keeper)
        _ (when-not (zk/exists client election-path)
            (zk/create-all client election-path :persistent? true))
        me (node-from-path (zk/create client (str election-path "/n-") :sequential? true)
                           election-path)
        group-member (ref {:me me
                           :keeper keeper
                           :client client
                           :election-path election-path
                           :leader-notify-fn leader-notify-fn})]
    (elect-leader group-member)))
