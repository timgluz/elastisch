;; Copyright 2011-2016 Michael S. Klishin, Alex Petrov, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.


(ns clojurewerkz.elastisch.shield
  (:require [clojure.string :as string]
            [clojurewerkz.elastisch.rest :as rest-client])
  (:import [clojure.lang IPersistentList IPersistentMap]
           clojurewerkz.elastisch.rest.Connection))

;;TODO: should use Spec here to add some protective Speck
(defrecord ShieldUser
  [^String username
   ^String password
   ^IPersistentList roles
   ^String full_name
   ^String email
   ^IPersistentMap metadata])

(defrecord ShieldRole
  [^IPersistentList cluster
   ^IPersistentList indices ;;list of ShieldRoleIndex
   ^IPersistentList run_as])

(defrecord ShieldRoleIndex
  [^IPersistentList names
   ^IPersistentList privileges
   ^IPersistentList fields
   ^IPersistentList query])

(defn remove-empty-fields
  "removes dictionary items with nil or empty value"
  [data-doc]
  (->> data-doc
    (remove
      (fn [[k v]]
        (or (nil? v)
            (and (coll? v) (empty? v)))))
    (into {})))

(defn init-user
  "Initializes a new user data map for request"
  ([^IPersistentMap shield-user-table]
    (map->ShieldUser shield-user-table))
  ([^String username ^String password]
    (init-user {:username username
                :password password}))
  ([^String username ^String password ^IPersistentList roles]
    (init-user {:username username
                :password password
                :roles (vec roles)})))

(defn init-role
  "initializes a new native Shield role"
  [clusters shield-indices]
  (->ShieldRole clusters 
                (vec (map #(map->ShieldRoleIndex %) shield-indices))
                []))

(defn add-user
  "creates a new native user.
  Params:
  @rest-client - initialized elastisch REST-client 
  @username  - string, 1 < len < 30
  @password  - string, 6 < len
  @roles     - vector, 1 < len"
  ([^Connection rest-conn ^ShieldUser shield-user ^ShieldUser new-user]
    (let [shield-uri (rest-client/url-with-path rest-conn "_shield/user" (:username new-user))]
      (rest-client/post rest-conn
                        shield-uri
                        {:basic-auth ((juxt :username :password) shield-user)
                         :body (remove-empty-fields (dissoc new-user :username))
                         :throw-exceptions false}))))

(defn get-users
  ([^Connection rest-conn ^ShieldUser shield-user]
    (rest-client/get rest-conn
                     (rest-client/url-with-path rest-conn "_shield/user")
                     {:basic-auth ((juxt :username :password) shield-user)}))
  ([^Connection rest-conn ^ShieldUser shield-user ^IPersistentList user-names]
    (rest-client/get rest-conn
                     (rest-client/url-with-path rest-conn "_shield/user" (string/join "," user-names))
                     {:basic-auth ((juxt :username :password) shield-user)})))

(defn delete-user
  [^Connection rest-conn ^ShieldUser shield-user ^String user-name]
  (rest-client/delete rest-conn
                      (rest-client/url-with-path rest-conn "_shield/user" user-name)
                      {:basic-auth ((juxt :username :password) shield-user)}))


(defn add-role
  "adds a new native Shield role
  more details: https://www.elastic.co/guide/en/shield/current/defining-roles.html"
  [^Connection rest-conn ^ShieldUser shield-user ^String role-name ^ShieldRole role]
  (rest-client/post
    rest-conn
    (rest-client/url-with-path rest-conn "_shield/role" role-name)
    {:basic-auth ((juxt :username :password) shield-user)
     :body (remove-empty-fields role)}))

(defn get-roles
  "fetches a list of Shield roles"
  [^Connection rest-conn ^ShieldUser shield-user]
  (rest-client/get rest-conn
                   (rest-client/url-with-path rest-conn "_shield/role")
                   {:basic-auth ((juxt :username :password) shield-user)}))

(defn delete-role
  "deletes the role by its name"
  [^Connection rest-conn ^ShieldUser shield-user ^String role-name]
  (rest-client/delete rest-conn
                      (rest-client/url-with-path rest-conn "_shield/role" role-name)
                      {:basic-auth ((juxt :username :password) shield-user)}))


(comment
  (require '[clojurewerkz.elastisch.rest :as rest-client] :reload)

  (def conn (rest-client/connect))

  (require '[clojurewerkz.elastisch.shield :as shield] :reload)
  (def shield-user (shield/init-user "es_admin" "toor123"))
  (println shield-user)

  (def test-user (shield/init-user "es_test" "qwerty123" ["test"]))

  (shield/add-user conn shield-user test-user)
  (shield/get-users conn shield-user)
  (shield/get-users conn shield-user ["es_test"]) 
  (shield/delete-user conn shield-user (:username test-user)) 
  )
