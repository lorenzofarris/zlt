(defproject zlt "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.3.0"]
		 [org.clojure/clojure-contrib "1.2.0"]
		 [ring/ring-core "0.3.11"]
		 [ring/ring-jetty-adapter "0.3.11"]
		 [enlive "1.0.0"]
		 [compojure "0.6.5"]
		 [log4j "1.2.16"]
                 [clojureql "1.0.0"]
                 [com.h2database/h2 "1.3.160"]]
  :dev-dependencies [[ring/ring-devel "0.3.11"]
		     [lein-ring "0.4.5"]
                     [swank-clojure "1.3.2"]]
  :ring {:handler zlt.core/app})
