(ns sneer.async
  (:require [clojure.core.async :as async :refer [chan go]]))

(def IMMEDIATELY (doto (async/chan) async/close!))

(defn dropping-chan [& [n]]
  (chan (async/dropping-buffer (or n 1))))

(defn sliding-chan [& [n]]
  (chan (async/sliding-buffer (or n 1))))

(defmacro go-trace
  [& forms]
  `(go
     (try
       ~@forms
       (catch java.lang.Exception ~'e
         (println "GO ERROR" ~'e)
         (. ~'^Exception e printStackTrace)))))

(defmacro while-let
  "Makes it easy to continue processing an expression as long as it is true"
  [binding & forms]
  `(loop []
     (when-let ~binding
       ~@forms
       (recur))))

(defmacro go-while-let
  "Makes it easy to continue processing data from a channel until it closes"
  [binding & forms]
  `(go-trace
     (while-let ~binding
                ~@forms)))