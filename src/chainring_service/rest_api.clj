
;  (C) Copyright 2017, 2018  Pavel Tisnovsky
;
;  All rights reserved. This program and the accompanying materials
;  are made available under the terms of the Eclipse Public License v1.0
;  which accompanies this distribution, and is available at
;  http://www.eclipse.org/legal/epl-v10.html
;
;  Contributors:
;      Pavel Tisnovsky
;

(ns chainring-service.rest-api
    "Handlers for all REST API calls.

    Author: Pavel Tisnovsky")


(require '[clojure.tools.logging                   :as log])

(require '[chainring-service.db-interface          :as db-interface])
(require '[chainring-service.config                :as config])
(require '[chainring-service.drawings-storage      :as drawings-storage])
(require '[chainring-service.drawings-cache        :as drawings-cache])

(require '[chainring-service.sap-interface         :as sap-interface])
(require '[chainring-service.mocked-sap-interface  :as mocked-sap-interface])
(require '[chainring-service.rest-api-utils        :as rest-api-utils])
(require '[chainring-service.rest-api-impl         :as rest-api-impl])

(use     '[clj-utils.utils])


(defn unknown-endpoint-handler
    "Process any unknown endpoints."
    [request uri]
    (rest-api-utils/send-error-response "unknown endpoint" uri request :bad-request))


(defn toplevel-handler
    "REST API handler for the /api endpoint."
    [request api-full-prefix]
    (let [response {api-full-prefix "current REST API version endpoint"}]
        (rest-api-utils/send-response response request)))


(defn api-info-handler
    "REST API handler for the /api/{version} endpoint."
    [request api-prefix]
    (let [response {(str api-prefix "/")               "the schema"
                    (str api-prefix "/info")           "basic info about the service"
                    (str api-prefix "/liveness")       "check the liveness of the service"
                    (str api-prefix "/readiness")      "check the readiness of the service and all subcomponents"
                    (str api-prefix "/config")         "actual configuration"
                    (str api-prefix "/areals")         "list of areals"
                    (str api-prefix "/buildings")      "list of buildings"
                    (str api-prefix "/floors")         "list of floors"
                    (str api-prefix "/rooms")          "list of rooms"
                    (str api-prefix "/areal")          "areal metadata"
                    (str api-prefix "/building")       "building metadata"
                    (str api-prefix "/floor")          "floor metadata"
                    (str api-prefix "/drawing")        "drawing metadata"
                    (str api-prefix "/drawings-cache") "drawings cache statistic"}]
        (rest-api-utils/send-response response request)))


(defn info-handler
    "REST API handler for the /api/{version}/info endpoint."
    [request]
    (let [response {:name            "Chainring Service"
                    :service-version (config/get-version request)
                    :db-version      (config/get-db-version request)
                    :api-prefix      (config/get-api-prefix request)
                    :api-version     (config/get-api-version request)
                    :full-prefix     (config/get-api-full-prefix request)}]
                    ;:hostname   hostname :test "/api"}]
        (rest-api-utils/send-response response request)))


(defn liveness-handler
    "REST API handler for the /api/{version}/liveness endpoint."
    [request]
    (let [response {:status "ok"}]
        (rest-api-utils/send-response response request)))


(defn readiness-handler
    "REST API handler for the /api/{version}/readiness endpoint."
    [request]
    (let [response {:status "ok"}]
        (rest-api-utils/send-response response request)))


(defn config-handler
    "REST API handler for the /api/{version}/config endpoint."
    [request]
    (let [response {:configuration (:configuration request)}]
        (rest-api-utils/send-response response request)))


(defn list-all-aoids
    "REST API handler for the /api/{version}/aoids endpoint."
    [request uri]
    (try
        (let [params     (:params request)
              valid-from (get params "valid-from")]
              (if (rest-api-utils/valid-date? valid-from)
                  (rest-api-utils/send-response (rest-api-impl/all-aoids request valid-from) request)
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "list-all-aoids")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn list-all-objects
    "REST API handler for the /api/{version}/objects endpoint."
    [request uri]
    (try
        (let [params     (:params request)
              valid-from (get params "valid-from")]
              (if (rest-api-utils/valid-date? valid-from)
                  (rest-api-utils/send-response (rest-api-impl/all-objects request valid-from) request)
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "list-all-objects")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn list-of-areals-handler
    "REST API handler for the /api/{version}/areals endpoint."
    [request uri]
    (try
        (let [params     (:params request)
              valid-from (get params "valid-from")]
              (if (rest-api-utils/valid-date? valid-from)
                  (rest-api-utils/send-response (rest-api-impl/areals request valid-from) request)
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "list-of-areals")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn list-of-buildings-handler
    "REST API handler for the /api/{version}/buildings endpoint."
    [request uri]
    (try
        (let [params     (:params request)
              areal-id   (get params "areal-id")
              valid-from (get params "valid-from")]
              (if (rest-api-utils/valid-date? valid-from)
                  (rest-api-utils/send-response (rest-api-impl/buildings request areal-id valid-from) request)
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "list-of-buildings")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn list-of-floors-handler
    "REST API handler for the /api/{version}/floors endpoint."
    [request uri]
    (try
        (let [params      (:params request)
              areal-id    (get params "areal-id")
              building-id (get params "building-id")
              valid-from  (get params "valid-from")]
              (if (rest-api-utils/valid-date? valid-from)
                  (rest-api-utils/send-response (rest-api-impl/floors request areal-id building-id valid-from) request)
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "list-of-floors")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn list-of-rooms-handler
    "REST API handler for the /api/{version}/rooms endpoint."
    [request uri]
    (try
        (let [params      (:params request)
              areal-id    (get params "areal-id")
              building-id (get params "building-id")
              floor-id    (get params "floor-id")
              valid-from  (get params "valid-from")]
              (if (rest-api-utils/valid-date? valid-from)
                  (rest-api-utils/send-response (rest-api-impl/rooms request areal-id building-id floor-id valid-from) request)
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "list-of-rooms")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn info-about-areal-handler
    "REST API handler for /api/{version}/areal endpoint."
    [request uri]
    (try
        (let [params     (:params request)
              valid-from (get params "valid-from") 
              areal-id   (get params "areal-id")]
              (if (rest-api-utils/valid-date? valid-from)
                  (if areal-id
                      (rest-api-utils/send-response (rest-api-impl/areal request areal-id valid-from) request)
                      (rest-api-utils/send-error-response "areal-id parameter is not specified" uri request :bad-request))
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "info-about-areal")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn info-about-building-handler
    "REST API handler for the /api/{version}/building endpoint."
    [request uri]
    (try
        (let [params        (:params request)
              valid-from    (get params "valid-from") 
              building-id   (get params "building-id")]
              (if (rest-api-utils/valid-date? valid-from)
                  (if building-id
                      (rest-api-utils/send-response (rest-api-impl/building request building-id valid-from) request)
                      (rest-api-utils/send-error-response "building-id parameter is not specified" uri request :bad-request))
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "info-about-building")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn info-about-floor-handler
    "REST API handler for the /api/{version}/floor endpoint."
    [request uri]
    (try
        (let [params        (:params request)
              valid-from    (get params "valid-from") 
              floor-id      (get params "floor-id")]
              (if (rest-api-utils/valid-date? valid-from)
                  (if floor-id
                      (rest-api-utils/send-response (rest-api-impl/floor request floor-id valid-from) request)
                      (rest-api-utils/send-error-response "floor-id parameter is not specified" uri request :bad-request))
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "info-about-floor")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn info-about-room-handler
    "REST API handler for the /api/{version}/room endpoint."
    [request uri]
    (try
        (let [params       (:params request)
              valid-from   (get params "valid-from") 
              room-id      (get params "room-id")]
              (if (rest-api-utils/valid-date? valid-from)
                  (if room-id
                      (rest-api-utils/send-response (rest-api-impl/room request room-id valid-from) request)
                      (rest-api-utils/send-error-response "room-id parameter is not specified" uri request :bad-request))
                  (rest-api-utils/send-error-response-wrong-date valid-from uri request)))
        (catch Exception e
            (log/error e "info-about-room")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


; ------------------------------------------------


(defn list-all-dates-from
    "Returns all dates (with drawings) from the selected date."
    [request uri]
    (try
        (let [start-time (System/currentTimeMillis)
              dates      (sap-interface/call-sap-interface request "read-all-dates-from")
              end-time   (System/currentTimeMillis)
              timestamp  (rest-api-utils/get-timestamp)
              response {:status     :ok
                        :duration   (- end-time start-time)
                        :timestamp  timestamp
                        :dates-from dates}]
            (rest-api-utils/send-response response request))
        (catch Exception e
            (log/error e "read-dates-from")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))


(defn nearest-date-from
    "Returns nearest date from the specified date-from."
    [request uri]
    (try
        (let [start-time      (System/currentTimeMillis)
              params          (:params request)
              date-from       (get params "date-from")
              real-date-from  (sap-interface/call-sap-interface request "get-real-date-from" date-from)
              end-time        (System/currentTimeMillis)
              timestamp       (rest-api-utils/get-timestamp)
              response {:status     :ok
                        :duration   (- end-time start-time)
                        :timestamp  timestamp
                        :date-from  real-date-from}]
            (rest-api-utils/send-response response request))
        (catch Exception e
            (log/error e "nearest-date-from")
            (rest-api-utils/send-error-response "SAP Access error" (str e) request :internal-server-error))))



(defn drawing-handler
    "REST API handler for the /api/{version}/drawing endpoint."
    [request uri]
    (let [params        (:params request)
          drawing-id    (get params "drawing-id")
          drawing-info  (db-interface/read-drawing-info drawing-id)
          rooms         (db-interface/read-room-list drawing-id)
          result        {:drawing-info drawing-info
                         :rooms rooms}]
          (log/info "Drawing ID:" drawing-id)
          (log/info "Drawing info" drawing-info)
          (log/info "Rooms" rooms)
          (if drawing-id
              (if drawing-info
                  (rest-api-utils/send-response result request)
                  (rest-api-utils/send-error-response "no drawing info" uri request :internal-server-error))
              (rest-api-utils/send-error-response "you need to specify drawing ID" uri request :internal-server-error))))


(defn sap-reload-mock-data
    "Reload all data used to mock SAP responses."
    [request uri]
    (let [mock-sap-response (config/mock-sap-response? request)]
          (if mock-sap-response
              (let [status (mocked-sap-interface/reload-mock-data)]
                    (rest-api-utils/send-response status request)))))


(defn get-sap-href
    "Get the HREF to SAP prefix from the configuration."
    [configuration object-type]
    (condp = object-type
        "room" (str "SAPEVENT:ROOM_CLICK?" (:param-name-to-sap-room-id configuration) "=")
        ""))


(defn get-sap-selector
    "Get the SAP selector from the URI."
    [uri]
    (if (.contains uri "/")
        (subs uri (inc (.lastIndexOf uri "/")))))


(defn sap-href-handler
    "REST API handler for the /api/{version}/sap-href endpoint."
    [request uri]
    (let [configuration (-> request :configuration :sap-interface)
          object-type   (get-sap-selector uri)
          href          (get-sap-href configuration object-type)]
          (rest-api-utils/send-response href request)))


(defn sap-debug-handler
    "REST API handler for the /api/{version}/sap-debug endpoint."
    [request uri]
    (let [params    (:params request)]
         (log/info "fake SAP handler")
         (log/info params)
         (rest-api-utils/send-response params request)))


(defn rooms-attribute
    "Returns list of selected attribute values for all rooms on floor."
    [request uri]
    (let [params     (:params request)
          floor      (get params "floor-id")
          valid-from (get params "valid-from")
          attribute  (get params "attribute")
          sap-response (sap-interface/call-sap-interface request "read-rooms-attribute" floor valid-from attribute)]
          (println "-------------")
          (println (count sap-response))
          (println "~~~~~~~~~~~~~")
          (println sap-response)
          (println "-------------")
          (rest-api-utils/send-response sap-response request)
       ;   (let [cookie-val (clojure.string/join "_" (for [r sap-response] (str (:AOID r) "|" (:value r) "|" (:key r))))]
       ; (rest-api-utils/send-response-with-cookie sap-response request :ok "rooms" cookie-val))
    ))


(defn possible-attributes
    "Returns set of possible attribute values for all rooms on floor."
    [request uri]
    (let [params     (:params request)
          floor      (get params "floor-id")
          valid-from (get params "valid-from")
          attribute  (get params "attribute")
          sap-response (sap-interface/call-sap-interface request "read-rooms-possible-attributes" floor valid-from attribute)]
          (println "-------------")
          (println (count sap-response))
          (println "~~~~~~~~~~~~~")
          (println sap-response)
          (println "-------------")
          (let [cookie-val (clojure.string/join "_" (for [r sap-response] r))]
        (rest-api-utils/send-response-with-cookie sap-response request :ok "possible_attributes" cookie-val)
    )))


(defn all-drawings-handler
    "REST API handler for the /api/{version}/drawings endpoint."
    [request uri]
    (let [buildings (db-interface/read-all-buildings)
          floors    (db-interface/read-all-floors)
          drawings  (db-interface/read-all-drawings)
          response  {
                     :buildings buildings
                     :floors    floors
                     :drawings  drawings}
         ]
         (rest-api-utils/send-response response request)))


(defn drawings-cache-info-handler
    "REST API handler for the /api/{version}/drawings-cache endpoint."
    [request]
    (let [response {:cache-utilization @drawings-cache/hit-counters
                    :cache-size (drawings-cache/cache-size)}]
         (rest-api-utils/send-response response request)))


(defn store-drawing-raw-data
    "REST API handler for the /api/{version}/drawing-raw-data endpoint."
    [request]
    (let [params     (:params request)
          drawing-id (get params "drawing")
          raw-data   (rest-api-utils/read-request-body request)]
        (if (and drawing-id raw-data)
            (try
                (db-interface/store-drawing-raw-data drawing-id raw-data)
                (rest-api-utils/send-ok-response "Drawing has been written into the database" request)
                (catch Exception e
                    (log/error e)
                    (rest-api-utils/send-error-response "exception occured during write" (.getMessage e) request :internal-server-error)))
            (rest-api-utils/send-error-response "send drawing ID as parameter and raw data in the body" "wrong input" request :bad-request)
        )))


(defn missing-parameter
    "Send error response when some required parameter is missing."
    [request parameter]
    (let [message (str "missing required parameter '" parameter "'")]
        (rest-api-utils/send-error-response message "wrong input" request :bad-request)))


(defn try-to-load-drawing
    "Try to load the drawing from the filesystem."
    [drawing-id store-format configuration]
    (let [id        (parse-int drawing-id)
          directory (-> configuration :drawings :directory)]
          {"directory" directory}))


(defn deserialize-drawing
    "Deserialize the drawing from the filesystem."
    [request]
    (let [params        (:params request)
          configuration (:configuration request)
          drawing-id    (get params "drawing")
          store-format  (get params "format")]
          (cond (and drawing-id format)
                (try
                    (-> (try-to-load-drawing drawing-id store-format configuration)
                        (rest-api-utils/send-response request 200))
                    (catch Exception e
                        (log/error e)
                        (rest-api-utils/send-error-response "exception occured during write" (.getMessage e) request :internal-server-error)))
                (nil? drawing-id)
                (missing-parameter request "drawing")
                (nil? store-format)
                (missing-parameter request "format")
    )))


(defn try-to-store-drawing
    "Try to store the drawing onto the filesystem."
    [drawing-id store-format raw-data configuration]
    (let [directory (-> configuration :drawings :directory)]
          (drawings-storage/store-drawing-as drawing-id directory store-format raw-data)
          (drawings-cache/delete drawing-id)))


(defn serialize-drawing
    "Serialize the drawing onto the filesystem. Drawing data is sent in JSON format in the body of the POST request."
    [request]
    (let [params        (:params request)
          configuration (:configuration request)
          drawing-id    (get params "drawing")
          store-format  (get params "format" "json")
          raw-data      (rest-api-utils/read-request-body request)]
          (cond (and drawing-id store-format raw-data)
                (try
                    (try-to-store-drawing drawing-id store-format raw-data configuration)
                    (rest-api-utils/send-ok-response "Drawing has been saved" request)
                    (catch Exception e
                        (log/error e)
                        (rest-api-utils/send-error-response "exception occured during write" (.getMessage e) request :internal-server-error)))
                (nil? drawing-id)
                (missing-parameter request "drawing")
                (nil? store-format)
                (missing-parameter request "format")
                (nil? raw-data)
                (rest-api-utils/send-error-response "missing body with drawing data" "wrong input" request :bad-request))))
