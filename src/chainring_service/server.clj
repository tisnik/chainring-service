;
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

(ns chainring-service.server
    "Server module with functions to accept requests and send response back to users via HTTP.

    Author: Pavel Tisnovsky")

(require '[ring.util.response      :as http-response])
(require '[clojure.tools.logging   :as log])

(require '[clj-fileutils.fileutils :as fileutils])

(require '[chainring-service.db-interface       :as db-interface])
(require '[chainring-service.html-renderer      :as html-renderer])
(require '[chainring-service.html-renderer-help :as html-renderer-help])
(require '[chainring-service.rest-api           :as rest-api])
(require '[chainring-service.raster-renderer    :as raster-renderer])
(require '[chainring-service.vector-drawing     :as vector-drawing])
(require '[chainring-service.config             :as config])
(require '[chainring-service.http-utils         :as http-utils])
(require '[chainring-service.sap-interface      :as sap-interface])
(require '[chainring-service.real-sap-interface :as real-sap-interface])
(require '[chainring-service.drawing-utils      :as drawing-utils])

(use     '[clj-utils.utils])


(defn get-user-id
    "Get user name: it can be read from cookies or generated by database."
    [request]
    (let [cookies (:cookies request)]
        (or (get (get cookies "user-id") :value)
            (db-interface/get-new-user-id))))


(defn finish-processing
    "Finish processing of HTTP requests."
    (   [request response-html]
        ; use the previous session as the new one
        (finish-processing request response-html (:session request)))
    (   [request response-html session]
        (let [cookies   (:cookies request)
              user-id   (get-user-id request)]
            (log/info "Incoming cookies: " cookies)
            (log/info "user-id:          " user-id)
            (-> (http-response/response response-html)
                (http-response/set-cookie "user-id" user-id {:max-age 36000000})
                ; use the explicitly specified new session with a map of values
                (assoc :session session)
                (http-response/content-type "text/html; charset=utf-8")))))


(defn current-date-formatted
    "Return current date formatted as yyyy-MM-dd."
    []
    (let [timeformatter (new java.text.SimpleDateFormat "yyyy-MM-dd")
          today         (new java.util.Date)]
          (.format timeformatter today)))


(defn process-front-page
    "Function that prepares data for the front page."
    [request]
    (let [valid-from    (current-date-formatted)]
        (finish-processing request (html-renderer/render-front-page valid-from))))


(defn process-settings-page
    "Function that prepares data for the settings page."
    [request]
    (let [user-id (get-user-id request)]
        (finish-processing request (html-renderer/render-settings-page user-id))))


(defn process-store-settings-page
    "Function that prepares data for the store settings page."
    [request]
    (let [params              (:params request)
          user-id             (get params "user-id")
          resolution          (get params "resolution")
          selected-room-color (get params "selected-room-color-code")
          pen-width           (get params "pen-width")]
          (println "*******" user-id resolution selected-room-color pen-width)
          (try
              (db-interface/store-user-settings user-id resolution selected-room-color pen-width)
              (finish-processing request (html-renderer/render-store-settings-page))
              (catch Exception e
                  (log/error e)
                  (finish-processing request (html-renderer/render-error-page
                                              (str "Chyba při zápisu do databáze: " (.getMessage e))))))))

(defn process-db-statistic-page
    "Function that prepares data for the database statistic page."
    [request]
    (let [last-update   (new java.util.Date)
          timeformatter (new java.text.SimpleDateFormat "yyyy-MM-dd")
          now           (new java.util.Date)
          now-str       (.format timeformatter now)
          areals        (count (real-sap-interface/read-areals now-str))
          buildings     (count (real-sap-interface/read-buildings nil now-str))
          floors        (count (real-sap-interface/read-floors nil nil now-str))
          rooms         (count (real-sap-interface/read-rooms nil now-str))
          drawings      (count (fileutils/filename-list "drawings/" ".json"))]

        ;(log/info "db stats" db-stats)
        (finish-processing request (html-renderer/render-db-statistic-page last-update areals buildings floors rooms drawings))))


(defn process-drawings-statistic-page
    "Function that prepares data for the drawings statistic page."
    [request]
    (let [drawings-count (count (fileutils/filename-list "drawings/" ".drw"))
          json-count (count (fileutils/filename-list "drawings/" ".json"))
          binary-count (count (fileutils/filename-list "drawings/" ".bin"))]
        (log/info "drawings counts" drawings-count)
        (log/info "json counts" json-count)
        (log/info "binary counts" binary-count)
        (finish-processing request (html-renderer/render-drawings-statistic-page drawings-count json-count binary-count))))


(defn process-drawings-list
    "Function that prepares data for the page with list of drawings."
    [request]
    (let [drawings (fileutils/file-list "drawings/" ".drw")]
        (log/info "drawings" drawings)
        (finish-processing request (html-renderer/render-drawings-list drawings))))


(defn process-json-list
    "Function that prepares data for the page with list of drawings stored as JSON."
    [request]
    (let [jsons (fileutils/file-list "drawings/" ".json")]
        (log/info "json drawings" jsons)
        (finish-processing request (html-renderer/render-json-list jsons))))


(defn process-binary-list
    "Function that prepares data for the page with list of drawings stored in binary format"
    [request]
    (let [binaries (fileutils/file-list "drawings/" ".bin")]
        (log/info "binary drawings" binaries)
        (finish-processing request (html-renderer/render-binary-list binaries))))


(defn process-test
    "Function that prepares data for the test page."
    [request]
    (http-utils/return-file "www" "test.html" "text/html"))


(defn process-help-page
    "Function to render proper help page."
    [request page-handler]
    (finish-processing request (page-handler)))


(defn process-areal-list-page
    "Function that prepares data for the page with list of areals."
    [request]
    (let [params        (:params request)
          valid-from    (get params "valid-from")
          areals-struct (sap-interface/call-sap-interface request "read-areals" valid-from)
          areals-from   (:date-from areals-struct)
          areals        (:areals areals-struct)]
        (log/info "Areals" areals)
        (log/info "Valid from" valid-from)
        (if areals
            (if (seq areals)
                (finish-processing request (html-renderer/render-areals-list valid-from areals-from areals))
                (finish-processing request (html-renderer/render-error-page "Databáze projektů (areálů) je prázdná")))
            (finish-processing request (html-renderer/render-error-page "Chyba při přístupu k SAPu")))))


(defn process-areal-info-page
    "Function that prepares data for the page with information about selected areal."
    [request]
    (let [params         (:params request)
          areal-id       (get params "areal-id")
          valid-from     (get params "valid-from")
          areal-info     (sap-interface/call-sap-interface request "read-areal-info" areal-id valid-from)
          building-count (count (sap-interface/call-sap-interface request "read-buildings" areal-id valid-from))]
          (log/info "Areal ID:" areal-id)
          (log/info "Building count:" building-count)
          (log/info "Areal info" areal-info)
          (log/info "Valid from" valid-from)
          (if areal-id
              (if areal-info
                  (finish-processing request (html-renderer/render-areal-info areal-id areal-info building-count valid-from))
                  (finish-processing request (html-renderer/render-error-page "Nelze načíst informace o vybraném areálu")))
              (finish-processing request (html-renderer/render-error-page "Žádný areál nebyl vybrán")))))


(defn process-building-info-page
    "Function that prepares data for the page with information about selected building"
    [request]
    (let [params         (:params request)
          areal-id       (get params "areal-id")
          valid-from     (get params "valid-from")
          building-id    (get params "building-id")
          building-info  (sap-interface/call-sap-interface request "read-building-info" areal-id building-id valid-from)]
          ;floor-count    (count (sap-interface/call-sap-interface request "read-floors" areal-id building-id valid-from))]
          (log/info "Areal ID:" building-id)
          (log/info "Building ID:" building-id)
          ;(log/info "Floor count:" floor-count)
          (log/info "Building info" building-info)
          (log/info "Valid from" valid-from)
          (if building-id
              (if building-info
                  (finish-processing request (html-renderer/render-building-info building-id building-info valid-from))
                  (finish-processing request (html-renderer/render-error-page "Nelze načíst informace o vybrané budově")))
              (finish-processing request (html-renderer/render-error-page "Žádná budova nebyla vybrána")))))


(defn floor-id->str
    "Converts floor-id into string."
    [floor-id]
    (-> floor-id
        (clojure.string/replace \. \_)
        (clojure.string/replace \\ \_)
        (clojure.string/replace \/ \_)))


(defn all-drawings-for-floor
    "Prepares list of all drawings for specified floor."
    [floor-id]
    (let [files     (fileutils/file-list "drawings/" ".json")
          filenames (for [file files] (.getName file))
          floor-id  (floor-id->str floor-id)
          drawings  (filter #(startsWith % floor-id) filenames)]
          (sort drawings)))


(defn process-floor-info-page
    "Function that prepares data for the page with information about selected floor."
    [request]
    (let [params                (:params request)
          areal-id              (get params "areal-id")
          building-id           (get params "building-id")
          floor-id              (get params "floor-id")
          valid-from            (get params "valid-from")
          floor-info            (sap-interface/call-sap-interface request "read-floor-info" areal-id building-id floor-id valid-from)
          drawings              (all-drawings-for-floor floor-id)]
          (log/info "Floor ID:" floor-id)
          (log/info "Drawings: " drawings)
          (log/info "Drawing count:" (count drawings))
          (log/info "Floor info" floor-info)
          (if floor-id
              (if floor-info
                  (finish-processing request (html-renderer/render-floor-info floor-id floor-info (count drawings) valid-from))
                  (finish-processing request (html-renderer/render-error-page "Nelze načíst informace o vybraném podlaží")))
              (finish-processing request (html-renderer/render-error-page "Žádné podlaží nebylo vybráno")))))


(defn entity-count
    "Count number of entities with specified entity type."
    [entities entity-type]
    (count (filter #(= entity-type (:T %)) entities)))


(defn prepare-drawing-info
    "Prepare statistic information about drawing."
    [drawing-id drawing-data]
    (let [entities (:entities drawing-data)]
        {:entities-count {:all       (:entities_count drawing-data)
                          :lines     (entity-count entities "L")
                          :circles   (entity-count entities "C")
                          :arcs      (entity-count entities "A")
                          :texts     (entity-count entities "T")
                          :polylines (entity-count entities "P")}
         :rooms-count    (:rooms_count drawing-data)
         :created        (:created drawing-data)
         :format-version (:version drawing-data)}))


(defn process-drawing-info
    "Function that prepares data for the page with information about selected drawing."
    [request]
    (let [params                (:params request)
          drawing-id            (get params "drawing-id")
          drawing-data          (raster-renderer/get-drawing-data drawing-id nil true)
          drawing-info          (prepare-drawing-info drawing-id drawing-data)]
          (log/info "Drawing ID:" drawing-id)
          (log/info "Drawing info" drawing-info)
          (if drawing-id
              (if drawing-info
                  (finish-processing request (html-renderer/render-drawing-info drawing-id drawing-info))
                  (finish-processing request (html-renderer/render-error-page "Nelze načíst informace o vybraném výkresu")))
              (finish-processing request (html-renderer/render-error-page "Žádný výkres nebyl vybrán")))))


(defn process-room-list
    "Function that prepares data for the page with list of rooms for selected floor."
    [request]
    (let [params     (:params request)
          floor-id   (get params "floor-id")
          floor-info (db-interface/read-floor-info floor-id)
          version    (get params "version")
          rooms      (db-interface/read-sap-room-list floor-id version)]
          (log/info "Floor ID:" floor-id)
          (log/info "Floor info" floor-info)
          (log/info "Rooms count:" (count rooms))
          (finish-processing request (html-renderer/render-room-list-page floor-id floor-info version rooms))
    ))


(defn process-areal-page
    "Function that prepares data for the page with list of buildings for selected areal"
    [request]
    (let [params       (:params request)
          areal-id     (get params "areal-id")
          valid-from   (get params "valid-from")
          areal-info   (sap-interface/call-sap-interface request "read-areal-info" areal-id valid-from)]
          (log/info "Areal ID:" areal-id)
          (log/info "Areal info:" areal-info)
          (log/info "Valid from" valid-from)
          (if areal-id
              (let [buildings (sap-interface/call-sap-interface request "read-buildings" areal-id valid-from)]
                  (log/info "Buildings:" buildings)
                  (if (seq buildings)
                      (finish-processing request (html-renderer/render-building-list areal-id areal-info buildings valid-from))
                      (finish-processing request (html-renderer/render-error-page "Nebyla nalezena žádná budova"))))
              (finish-processing request (html-renderer/render-error-page "Žádný areál nebyl vybrán")))))


(defn process-building-page
    "Function that prepares data for the page with list of floors for the selected building."
    [request]
    (let [params        (:params request)
          areal-id      (get params "areal-id")
          building-id   (get params "building-id")
          valid-from    (get params "valid-from")
          areal-info    (sap-interface/call-sap-interface request "read-areal-info" areal-id valid-from)
          building-info (sap-interface/call-sap-interface request "read-building-info" areal-id building-id valid-from)]
          (log/info "Areal ID:" areal-id)
          (log/info "Areal info" areal-info)
          (log/info "Building ID:" building-id)
          (log/info "Building info" building-info)
          (log/info "Valid from" valid-from)
          (if building-id
              (let [floors (sap-interface/call-sap-interface request "read-floors" areal-id building-id valid-from)]
                  (log/info "Floors" floors)
                  (if (seq floors)
                      (finish-processing request (html-renderer/render-floor-list areal-id building-id areal-info building-info floors valid-from))
                      (finish-processing request (html-renderer/render-error-page "Nebylo nalezeno žádné podlaží"))))
              (finish-processing request (html-renderer/render-error-page "Budova nebyla vybrána")))))


(defn process-floor-page
    "Function that prepares data for the page with list of floors."
    [request]
    (let [params        (:params request)
          areal-id      (get params "areal-id")
          building-id   (get params "building-id")
          floor-id      (get params "floor-id")
          valid-from    (get params "valid-from")
          areal-info    (sap-interface/call-sap-interface request "read-areal-info" areal-id valid-from)
          building-info (sap-interface/call-sap-interface request "read-building-info" areal-id building-id valid-from)
          floor-info    (sap-interface/call-sap-interface request "read-floor-info" areal-id building-id floor-id valid-from)]
          (log/info "Areal ID:" areal-id)
          (log/info "Areal info" areal-info)
          (log/info "Building ID:" building-id)
          (log/info "Building info" building-info)
          (log/info "Floor ID:" floor-id)
          (log/info "Floor info" floor-info)
          (log/info "Valid from" valid-from)
          (if floor-id
              (let [drawings (all-drawings-for-floor floor-id)]
                  (log/info "Drawings" drawings)
                  (if (seq drawings)
                      (finish-processing request (html-renderer/render-drawing-list areal-id building-id floor-id areal-info building-info floor-info valid-from drawings))
                      (finish-processing request (html-renderer/render-error-page "Nebyl nalezen žádný výkres"))))
              (finish-processing request (html-renderer/render-error-page "Podlaží nebylo vybráno")))))


(defn select-nearest-date
    "Select nearest date from the sorted list of input dates."
    [today dates]
    (let [nearest (->> dates
                  (filter #(not (neg? (compare today %))))
                  last)]
         (or nearest (first dates))))


(defn process-select-drawing-from-sap-page-
    "Function that prepares data for the page with list of floors."
    [request]
    (let [params        (:params request)
          floor-id      (get params "floor-id")]
          (log/info "Floor ID:" floor-id)
          (if floor-id
              (let [drawings (all-drawings-for-floor floor-id)]
                  (log/info "Drawings" drawings)
                  (if (seq drawings)
                      (finish-processing request (html-renderer/render-drawing-list-from-sap floor-id drawings))
                      (finish-processing request (html-renderer/render-error-page "Nebyl nalezen žádný výkres"))))
              (finish-processing request (html-renderer/render-error-page "Podlaží nebylo vybráno")))))


(defn process-raster-preview-page
    "Function that prepares simple drawing preview."
    [request]
    (let [params        (:params request)
          drawing-name  (get params "drawing-name")]
          (log/info "Drawing name:" drawing-name)
          (if drawing-name
              (finish-processing request (html-renderer/render-raster-preview drawing-name))
              (finish-processing request (html-renderer/render-error-page "Nebyl vybrán žádný výkres")))))


(defn log-process-drawing-info
    "Log all relevant information about selected drawing."
    [project-id project-info building-id building-info floor-id floor-info drawing-id drawing-info rooms]
    (log/info "Project ID:" project-id)
    (log/info "Project info" project-info)
    (log/info "Building ID:" building-id)
    (log/info "Building info" building-info)
    (log/info "Floor ID:" floor-id)
    (log/info "Floor info" floor-info)
    (log/info "Drawing ID:" drawing-id)
    (log/info "Drawing info" drawing-info)
    (log/info "Rooms" rooms))


(defn no-drawing-error-page
    "Error page when no drawing has been found (regular entry)."
    [request]
    (finish-processing request (html-renderer/render-error-page "Nebyl nalezen žádný výkres")))


(defn no-drawing-error-page-from-sap
    "Error page when no drawing has been found (entry from SAP)."
    [request]
    (finish-processing request (html-renderer/render-error-page-sap "Nebyl nalezen žádný výkres")))


(defn process-drawing-page
    "Function that prepares data for the page with selected drawing."
    [request]
    (let [params               (:params request)
          session              (:session request)
          configuration        (:configuration request)
          areal-id             (get params "areal-id")
          building-id          (get params "building-id")
          floor-id             (get params "floor-id")
          drawing-id           (get params "drawing-id")
          valid-from           (get params "valid-from")
          areal-info           (sap-interface/call-sap-interface request "read-areal-info" areal-id valid-from)
          building-info        (sap-interface/call-sap-interface request "read-building-info" areal-id building-id valid-from)
          floor-info           (sap-interface/call-sap-interface request "read-floor-info" areal-id building-id floor-id valid-from)
          drawing-info         '()
          rooms                (sap-interface/call-sap-interface request "read-rooms" floor-id valid-from)
          room-attribute-types (sap-interface/call-sap-interface request "read-room-attribute-types")
          session              (assoc session :drawing-id drawing-id)]
          (log-process-drawing-info areal-id areal-info building-id building-info floor-id floor-info drawing-id drawing-info rooms)
          (if drawing-id
              (if drawing-info
                  (finish-processing request (html-renderer/render-drawing configuration areal-id building-id floor-id drawing-id areal-info building-info floor-info drawing-info valid-from nil rooms room-attribute-types nil) session)
                  (no-drawing-error-page request))
              (no-drawing-error-page request))))


(defn process-drawing-from-sap-page
    "Function that prepares data for the page with selected drawing."
    [request]
    (let [params         (:params request)
          session        (:session request)
          configuration  (:configuration request)
          floor-id       (get params "floor-id")
          drawing-id     (get params "drawing-id")
          valid-from     (get params "valid-from")
          valid-from-fmt (get params "valid-from")

          rooms          (sap-interface/call-sap-interface request "read-rooms" floor-id valid-from)
          room-attribute-types (sap-interface/call-sap-interface request "read-room-attribute-types")
          session        (assoc session :drawing-id drawing-id)]
          (if drawing-id
              (finish-processing request (html-renderer/render-drawing configuration nil nil floor-id drawing-id nil nil nil nil valid-from valid-from-fmt rooms room-attribute-types true) session)
              (no-drawing-error-page-from-sap request))))


(defn process-select-drawing-from-sap-page
    "Function that prepares data for the page with list of floors."
    [request]
    (let [params         (:params request)
          session        (:session request)
          configuration  (:configuration request)
          floor-id       (get params "floor-id")
          valid-from-fmt (or (get params "valid-from") (current-date-formatted))
          valid-from     (clojure.string/replace valid-from-fmt "-" "")
          drawings       (all-drawings-for-floor floor-id)
          drawing-dates  (sort (map #(drawing-utils/filename->drawing-version % floor-id) drawings))
          selected-date  (select-nearest-date valid-from drawing-dates)
          drawing-id     (str (floor-id->str floor-id) "_" selected-date)]

          (if (and drawing-id (seq drawings))
              (let [rooms                (sap-interface/call-sap-interface request "read-rooms" floor-id valid-from)
                    room-attribute-types (sap-interface/call-sap-interface request "read-room-attribute-types")
                    session              (assoc session :drawing-id drawing-id)]
                   (finish-processing request (html-renderer/render-drawing configuration nil nil floor-id drawing-id nil nil nil nil valid-from valid-from-fmt rooms room-attribute-types true) session))
              (no-drawing-error-page-from-sap request))))


(defn get-api-part-from-uri
    "Get API part (string) from the full URI. The API part string should not starts with /"
    [uri prefix]
    (let [api-part (re-find #"/[^/]*" (subs uri (count prefix)))]
       (if (and api-part (startsWith api-part "/"))
           (subs api-part 1)
           api-part)))


(defn get-api-command
    "Retrieve the actual command from the API call."
    [uri prefix]
    (if uri
        (if (startsWith uri prefix)
            (let [uri-without-prefix (subs uri (count prefix))]
                (if (empty? uri-without-prefix) ; special handler for a call with / only
                    ""
                    (get-api-part-from-uri uri prefix))))))


(defn api-call-handler
    "This function is used to handle all API calls. Three parameters are expected:
     data structure containing HTTP request, string with URI, and the HTTP method."
    [request uri method prefix]
    (if (= uri prefix)
        (rest-api/api-info-handler request prefix)
        (condp = [method (get-api-command uri prefix)]
            ; toplevel
            [:get  ""]                       (rest-api/api-info-handler request prefix)

            ; common endpoints
            [:get  "info"]                   (rest-api/info-handler request)
            [:get  "liveness"]               (rest-api/liveness-handler request)
            [:get  "readiness"]              (rest-api/readiness-handler request)
            [:get  "config"]                 (rest-api/config-handler request)

            ; endpoints to return list of AOIDs
            [:get  "aoids"]                  (rest-api/list-all-aoids request uri)
            [:get  "objects"]                (rest-api/list-all-objects request uri)
            [:get  "areals"]                 (rest-api/list-of-areals-handler request uri)
            [:get  "buildings"]              (rest-api/list-of-buildings-handler request uri)
            [:get  "floors"]                 (rest-api/list-of-floors-handler request uri)
            [:get  "rooms"]                  (rest-api/list-of-rooms-handler request uri)

            ; endpoints to return information about selected AOID
            [:get  "areal"]                  (rest-api/info-about-areal-handler request uri)
            [:get  "building"]               (rest-api/info-about-building-handler request uri)
            [:get  "floor"]                  (rest-api/info-about-floor-handler request uri)
            [:get  "room"]                   (rest-api/info-about-room-handler request uri)

            ; endpoints to work with dates
            [:get  "dates-from"]             (rest-api/list-all-dates-from request uri)
            [:get  "nearest-date-from"]      (rest-api/nearest-date-from request uri)

            [:get  "rooms-attribute"]        (rest-api/rooms-attribute request uri)
            [:get  "possible-attributes"]    (rest-api/possible-attributes request uri)

            [:get  "drawing"]                (rest-api/drawing-handler request uri)
            [:get  "drawings"]               (rest-api/all-drawings-handler request uri)
            [:put  "drawing-raw-data-to-db"] (rest-api/store-drawing-raw-data request)
            [:get  "drawing-data"]           (rest-api/deserialize-drawing request)
            [:put  "drawing-data"]           (rest-api/serialize-drawing request)
            [:post "drawing-data"]           (rest-api/serialize-drawing request)
            [:get  "drawings-cache"]         (rest-api/drawings-cache-info-handler request)
            [:post "sap-reload-mock-data"]   (rest-api/sap-reload-mock-data request uri)
            [:get  "sap-href"]               (rest-api/sap-href-handler request uri)
            [:get  "sap-debug"]              (rest-api/sap-debug-handler request uri)
            [:get  "raster-drawing"]         (raster-renderer/raster-drawing request)
                                             (rest-api/unknown-endpoint-handler request uri)
        )))


(defn uri->file-name
    "Converts URI to file name."
    [uri]
    (subs uri (inc (.indexOf uri "/"))))


(defn gui-call-handler
    "This function is used to handle all GUI calls. Three parameters are expected:
     data structure containing HTTP request, string with URI, and the HTTP method."
    [request uri method]
    (cond (.endsWith uri ".gif")  (http-utils/return-file "www" (uri->file-name uri) "image/gif")
          (.endsWith uri ".png")  (http-utils/return-file "www" (uri->file-name uri) "image/png")
          (.endsWith uri ".ico")  (http-utils/return-file "www" (uri->file-name uri) "image/x-icon")
          (.endsWith uri ".css")  (http-utils/return-file "www" (uri->file-name uri) "text/css")
          (.endsWith uri ".js")   (http-utils/return-file "www" (uri->file-name uri) "application/javascript")
          (.endsWith uri ".htm")  (http-utils/return-file "www" (uri->file-name uri) "text/html")
          (.endsWith uri ".html") (http-utils/return-file "www" (uri->file-name uri) "text/html")
          :else
        (condp = uri
            ; common pages
            "/"                           (process-front-page request)
            "/settings"                   (process-settings-page request)
            "/store-settings"             (process-store-settings-page request)
            "/db-stats"                   (process-db-statistic-page request)
            "/drawings-stats"             (process-drawings-statistic-page request)

            ; AOID list pages
            "/areals"                     (process-areal-list-page request)
            "/areal"                      (process-areal-page request)
            "/building"                   (process-building-page request)
            "/floor"                      (process-floor-page request)

            ; AOID info pages
            "/areal-info"                 (process-areal-info-page request)
            "/building-info"              (process-building-info-page request)
            "/floor-info"                 (process-floor-info-page request)

            "/room-list"                  (process-room-list request)

            ; pages with drawings
            "/drawing"                    (process-drawing-page request)
            "/select-drawing-from-sap"    (process-select-drawing-from-sap-page request)
            "/drawing-from-sap"           (process-drawing-from-sap-page request)
            "/drawing-info"               (process-drawing-info request)

            "/raster-preview"             (process-raster-preview-page request)
            "/vector-drawing-as-drw"      (vector-drawing/vector-drawing-as-drw request)
            "/vector-drawing-as-json"     (vector-drawing/vector-drawing-as-json request)
            "/vector-drawing-as-binary"   (vector-drawing/vector-drawing-as-binary request)
            "/raster-drawing"             (raster-renderer/raster-drawing request)
            "/find-room-on-drawing"       (raster-renderer/find-room-on-drawing request)
            "/drawings-list"              (process-drawings-list request)
            "/json-list"                  (process-json-list request)
            "/binary-list"                (process-binary-list request)
            "/test"                       (process-test request)

            "/help_intreno"               (process-help-page request html-renderer-help/intreno)
            "/help_valid_from"            (process-help-page request html-renderer-help/valid-from)
            "/help_valid_from_areal"      (process-help-page request html-renderer-help/valid-from-areal)
            "/help_valid_to_areal"        (process-help-page request html-renderer-help/valid-to-areal)
            "/help_valid_from_building"   (process-help-page request html-renderer-help/valid-from-building)
            "/help_valid_to_building"     (process-help-page request html-renderer-help/valid-to-building)
            "/help_valid_from_settings"   (process-help-page request html-renderer-help/valid-from-settings)
            "/help_aoid_areal"            (process-help-page request html-renderer-help/aoid-areal)
            "/help_name_areal"            (process-help-page request html-renderer-help/name-areal)
            "/help_aoid_building"         (process-help-page request html-renderer-help/aoid-building)
            "/help_name_building"         (process-help-page request html-renderer-help/name-building)
            "/help_floor_count_building"  (process-help-page request html-renderer-help/floor-count-building)
            "/help_aoid_floor"            (process-help-page request html-renderer-help/aoid-floor)
            "/help_name_floor"            (process-help-page request html-renderer-help/name-floor)
            "/help_drawing_count_floor"   (process-help-page request html-renderer-help/drawing-count-floor)
            )))

(defn handler
    "Handler that is called by Ring for all requests received from user(s)."
    [request]
    (log/info "request URI:   " (:uri request))
    ;(log/info "configuration: " (:configuration request))
    (let [uri             (:uri request)
          method          (:request-method request)
          api-prefix      (config/get-api-prefix request)
          api-full-prefix (config/get-api-full-prefix request)]
          ;(println uri)
        (cond (= uri api-prefix)            (rest-api/toplevel-handler request api-full-prefix)
              (= uri (str api-prefix "/"))  (rest-api/toplevel-handler request api-full-prefix)
              (startsWith uri api-prefix)   (api-call-handler request uri method api-full-prefix)
              :else                         (gui-call-handler request uri method))))
