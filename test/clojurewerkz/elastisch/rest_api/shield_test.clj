;; Copyright (c) 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.


(ns clojurewerkz.elastisch.rest-api.shield-test
  (:require [clojurewerkz.elastisch.rest :as rest-client]
            [clojurewerkz.elastisch.shield :as shield]
            [clojurewerkz.elastisch.fixtures :as fx]
            [clojure.test :refer :all]))

(use-fixtures :each fx/reset-indexes)

(def es-admin {:username "es_admin"
               :password "toor123"})

(def es-test {:username "es_test1"
              :password "test123"
              :roles ["test_role"]})

(deftest ^{rest true} test-CRUD-new-user
  (testing "returns empty list when no users added"
    (let [conn (rest-client/connect)
          res (shield/get-users conn es-admin)]
      (is (empty? res))))

  (testing "creates a new user"
    (let [conn (rest-client/connect)
          res (shield/add-user conn es-admin es-test)]
      (is (false? (empty? res)))
      (is (contains? res :user))
      (is (true? (get-in res [:user :created])))))
  
  (testing "returns freshly added new user"
    (let [conn (rest-client/connect)
          res (shield/get-users conn es-admin)]
      (is (false? (empty? res)))
      (is (contains? res :es_test1))
      (is (= (:username es-test) (get-in res [:es_test1 :username])))))

  (testing "deletes user"
    (let [conn (rest-client/connect)
          res (shield/delete-user conn es-admin (:username es-test))]
      (is (false? (empty? res)))
      (is (contains? res :found))
      (is (true? (:found res))))))

