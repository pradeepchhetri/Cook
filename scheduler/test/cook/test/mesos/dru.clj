;;
;; Copyright (c) Two Sigma Open Source, LLC
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;  http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
(ns cook.test.mesos.dru
 (:use clojure.test)
 (:require [cook.mesos.dru :as dru]
           [cook.mesos.util :as util]
           [cook.mesos.share :as share]
           [cook.test.mesos.schema :as schema :refer (restore-fresh-database! create-dummy-job create-dummy-instance)]
           [datomic.api :as d :refer (q db)]))

(deftest test-compute-task-scored-task-pairs
  (testing "test1"
    (let [datomic-uri "datomic:mem://test-score-tasks"
          conn (schema/restore-fresh-database! datomic-uri)
          job1 (schema/create-dummy-job conn :user "ljin" :memory 10.0 :ncpus 10.0)
          job2 (schema/create-dummy-job conn :user "ljin" :memory 5.0  :ncpus 5.0)
          job3 (schema/create-dummy-job conn :user "ljin" :memory 15.0 :ncpus 25.0)
          job4 (schema/create-dummy-job conn :user "ljin" :memory 25.0 :ncpus 15.0)
          task1 (schema/create-dummy-instance conn job1 :instance-status :instance.status/running)
          task2 (schema/create-dummy-instance conn job2 :instance-status :instance.status/running)
          task3 (schema/create-dummy-instance conn job3 :instance-status :instance.status/running)
          task4 (schema/create-dummy-instance conn job4 :instance-status :instance.status/running)
          task-ent1 (d/entity (d/db conn) task1)
          task-ent2 (d/entity (d/db conn) task2)
          task-ent3 (d/entity (d/db conn) task3)
          task-ent4 (d/entity (d/db conn) task4)
          tasks [task-ent1 task-ent2 task-ent3 task-ent4]]
      (let [scored-task1 (dru/->ScoredTask task-ent1 0.4 10.0 10.0)
            scored-task2 (dru/->ScoredTask task-ent2 0.6 5.0 5.0)
            scored-task3 (dru/->ScoredTask task-ent3 1.6 15.0 25.0)
            scored-task4 (dru/->ScoredTask task-ent4 2.2 25.0 15.0)]
        (is (= [[task-ent1 scored-task1]
                [task-ent2 scored-task2]
                [task-ent3 scored-task3]
                [task-ent4 scored-task4]]
               (dru/compute-task-scored-task-pairs tasks {:mem 25.0 :cpus 25.0})))))))

(deftest test-init-dru-divisors
  (testing "test1"
    (let [datomic-uri "datomic:mem://test-init-dru-divisors"
          conn (schema/restore-fresh-database! datomic-uri)
          job1 (schema/create-dummy-job conn :user "ljin" :memory 10.0 :ncpus 10.0)
          job2 (schema/create-dummy-job conn :user "wzhao" :memory 10.0 :ncpus 10.0)
          job3 (schema/create-dummy-job conn :user "sunil" :memory 10.0 :ncpus 10.0)
          task1 (schema/create-dummy-instance conn job1 :instance-status :instance.status/running)
          task2 (schema/create-dummy-instance conn job2 :instance-status :instance.status/running)
          db (d/db conn)
          running-task-ents (util/get-running-task-ents db)
          pending-job-ents [(d/entity db job3)]]
      (let [_ (share/set-share! conn "default" :mem 25.0 :cpus 25.0)
            _ (share/set-share! conn "wzhao" :mem 10.0 :cpus 10.0)
            db (d/db conn)]
        (is (= {"ljin" {:mem 25.0 :cpus 25.0} "wzhao" {:mem 10.0 :cpus 10.0} "sunil" {:mem 25.0 :cpus 25.0}}
               (dru/init-user->dru-divisors db running-task-ents pending-job-ents)))))))

(comment (run-tests))
