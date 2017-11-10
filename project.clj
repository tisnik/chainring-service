(defproject chainring-service "0.1.0-SNAPSHOT"
    :description "FIXME: write description"
    :url "http://example.com/FIXME"
    :license {:name "Eclipse Public License"
              :url "http://www.eclipse.org/legal/epl-v10.html"}
    :dependencies [[org.clojure/clojure "1.7.0"]
                   [org.clojure/java.jdbc "0.3.5"]
                   [org.clojure/tools.cli "0.3.1"]
                   [org.xerial/sqlite-jdbc "3.7.2"]
                   [clojure-ini "0.0.1"]
                   [ring/ring-core "1.3.2"]
                   [ring/ring-jetty-adapter "1.3.2"]
                   [org.clojure/data.json "0.2.5"]
                   [hiccup "1.0.4"]
                   [org.clojure/tools.logging "0.3.1"]
                   [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                      javax.jms/jms
                                                      com.sun.jmdk/jmxtools
                                                      com.sun.jmx/jmxri]]
                   [org.slf4j/slf4j-log4j12 "1.6.6"]]
    :main ^:skip-aot chainring-service.core
    :plugins [[lein-ring "0.8.10"]
              [codox "0.8.11"]
              [lein-cloverage "1.0.6"]]
    :ring {:handler clojure-service.core/app}
    :target-path "target/%s"
    :profiles {:uberjar {:aot :all}})