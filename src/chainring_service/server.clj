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
    "Server module with functions to accept requests and send response back to users via HTTP.")

(require '[ring.util.response      :as http-response])
(require '[clojure.tools.logging   :as log])
(require '[clojure.pprint          :as pprint])

(require '[clj-fileutils.fileutils :as fileutils])

(require '[chainring-service.db-interface     :as db-interface])
(require '[chainring-service.html-renderer    :as html-renderer])
(require '[chainring-service.rest-api         :as rest-api])
(require '[chainring-service.drawing-renderer :as drawing-renderer])
(require '[chainring-service.config           :as config])
(require '[chainring-service.http-utils       :as http-utils])

(use     '[clj-utils.utils])

(defn get-user-id
    "Get user name: it can be read from cookies or generated by database."
    [request]
    (let [cookies (:cookies request)]
        (or (get (get cookies "user-id") :value)
            (db-interface/get-new-user-id))))

(defn finish-processing
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

(defn process-front-page
    "Function that prepares data for the front page."
    [request]
    (finish-processing request (html-renderer/render-front-page)))

(defn process-settings-page
    [request]
    (let [user-id (get-user-id request)]
        (finish-processing request (html-renderer/render-settings-page user-id))))

(defn process-store-settings-page
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
    [request]
    (let [db-stats (db-interface/get-db-status)]
        (log/info "db stats" db-stats)
        (finish-processing request (html-renderer/render-db-statistic-page db-stats))))

(defn process-drawings-statistic-page
    [request]
    (let [drawings-count (count (fileutils/filename-list "drawings/" ".drw"))
          json-count (count (fileutils/filename-list "drawings/" ".json"))]
        (log/info "drawings counts" drawings-count)
        (log/info "json counts" json-count)
        (finish-processing request (html-renderer/render-drawings-statistic-page drawings-count json-count))))

(defn process-drawings-list
    [request]
    (let [drawings (fileutils/file-list "drawings/" ".drw")]
        (log/info "drawings" drawings)
        (finish-processing request (html-renderer/render-drawings-list drawings))))

(defn process-json-list
    [request]
    (let [jsons (fileutils/file-list "drawings/" ".json")]
        (log/info "json drawings" jsons)
        (finish-processing request (html-renderer/render-json-list jsons))))

(defn process-project-list-page
    "Function that prepares data for the page with project list."
    [request]
    (let [projects      (db-interface/read-project-list)]
        (log/info "Projects:" projects)
        (if projects
            (if (seq projects)
                (finish-processing request (html-renderer/render-project-list projects))
                (finish-processing request (html-renderer/render-error-page "Databáze projektů je prázdná")))
            (finish-processing request (html-renderer/render-error-page "Chyba při přístupu k databázi")))))

(defn process-project-info-page
    [request]
    (let [params         (:params request)
          project-id     (get params "project-id")
          project-info   (db-interface/read-detailed-project-info project-id)
          building-count (db-interface/read-building-count-for-project project-id)]
          (log/info "Project ID:" project-id)
          (log/info "Building count:" building-count)
          (log/info "Project info" project-info)
          (println "----------------")
          (pprint/pprint (:session request))
          (println "----------------")
          (if project-id
              (if project-info
                  (finish-processing request (html-renderer/render-project-info project-id project-info building-count))
                  (finish-processing request (html-renderer/render-error-page "Nelze načíst informace o vybraném projektu")))
              (finish-processing request (html-renderer/render-error-page "Žádný projekt nebyl vybrán")))))

(defn process-building-info-page
    [request]
    (let [params         (:params request)
          building-id    (get params "building-id")
          building-info  (db-interface/read-building-info building-id)
          floor-count    (db-interface/read-floor-count-for-building building-id)]
          (log/info "Building ID:" building-id)
          (log/info "Floor count:" floor-count)
          (log/info "Building info" building-info)
          (if building-id
              (if building-info
                  (finish-processing request (html-renderer/render-building-info building-id building-info floor-count))
                  (finish-processing request (html-renderer/render-error-page "Nelze načíst informace o vybrané budově")))
              (finish-processing request (html-renderer/render-error-page "Žádná budova nebyla vybrána")))))

(defn process-floor-info-page
    [request]
    (let [params         (:params request)
          floor-id       (get params "floor-id")
          floor-info     (db-interface/read-floor-info floor-id)
          drawing-count  (db-interface/read-drawing-count-for-floor floor-id)]
          (log/info "Floor ID:" floor-id)
          (log/info "Drawing count:" drawing-count)
          (log/info "Floor info" floor-info)
          (if floor-id
              (if floor-info
                  (finish-processing request (html-renderer/render-floor-info floor-id floor-info drawing-count))
                  (finish-processing request (html-renderer/render-error-page "Nelze načíst informace o vybraném podlaží")))
              (finish-processing request (html-renderer/render-error-page "Žádné podlaží nebylo vybráno")))))

(defn process-project-page
    "Function that prepares data for the page with list of buildings for selected project"
    [request]
    (let [params       (:params request)
          project-id   (get params "project-id")
          project-info (db-interface/read-project-info project-id)]
          (log/info "Project ID:" project-id)
          (log/info "Project info" project-info)
          (if project-id
              (let [buildings (db-interface/read-building-list project-id)]
                  (log/info "Buildings:" buildings)
                  (if (seq buildings)
                      (finish-processing request (html-renderer/render-building-list project-id project-info buildings))
                      (finish-processing request (html-renderer/render-error-page "Nebyla nalezena žádná budova"))))
              (finish-processing request (html-renderer/render-error-page "Projekt nebyl vybrán")))))

(defn process-building-page
    "Function that prepares data for the page with list of floors for the selected building."
    [request]
    (let [params        (:params request)
          project-id    (get params "project-id")
          building-id   (get params "building-id")
          project-info  (db-interface/read-project-info project-id)
          building-info (db-interface/read-building-info building-id)]
          (log/info "Project ID:" project-id)
          (log/info "Project info" project-info)
          (log/info "Building ID:" building-id)
          (log/info "Building info" building-info)
          (if building-id
              (let [floors (db-interface/read-floor-list building-id)]
                  (log/info "Floors" floors)
                  (if (seq floors)
                      (finish-processing request (html-renderer/render-floor-list project-id building-id project-info building-info floors))
                      (finish-processing request (html-renderer/render-error-page "Nebylo nalezeno žádné podlaží"))))
              (finish-processing request (html-renderer/render-error-page "Budova nebyla vybrána")))))

(defn process-floor-page
    "Function that prepares data for the page with list of floors."
    [request]
    (let [params        (:params request)
          project-id    (get params "project-id")
          building-id   (get params "building-id")
          floor-id      (get params "floor-id")
          project-info  (db-interface/read-project-info project-id)
          building-info (db-interface/read-building-info building-id)
          floor-info    (db-interface/read-floor-info floor-id)]
          (log/info "Project ID:" project-id)
          (log/info "Project info" project-info)
          (log/info "Building ID:" building-id)
          (log/info "Building info" building-info)
          (log/info "Floor ID:" floor-id)
          (log/info "Floor info" floor-info)
          (if building-id
              (let [drawings (db-interface/read-drawing-list floor-id)]
                  (log/info "Drawings" drawings)
                  (if (seq drawings)
                      (finish-processing request (html-renderer/render-drawing-list project-id building-id floor-id project-info building-info floor-info drawings))
                      (finish-processing request (html-renderer/render-error-page "Nebyl nalezen žádný výkres"))))
              (finish-processing request (html-renderer/render-error-page "Budova nebyla vybrána")))))

(defn process-drawing-preview-page
    [request]
    (let [params        (:params request)
          session       (:session request)
          drawing-name  (get params "drawing")]
          (log/info "Drawing name:" drawing-name)
          (if drawing-name
              (finish-processing request (html-renderer/render-drawing-preview drawing-name))
              (finish-processing request (html-renderer/render-error-page "Nebyl vybrán žádný výkres")))))

(defn process-drawing-page
    "Function that prepares data for the page with selected drawing."
    [request]
    (let [params        (:params request)
          session       (:session request)
          project-id    (get params "project-id")
          building-id   (get params "building-id")
          floor-id      (get params "floor-id")
          drawing-id    (get params "drawing-id")
          project-info  (db-interface/read-project-info project-id)
          building-info (db-interface/read-building-info building-id)
          floor-info    (db-interface/read-floor-info floor-id)
          drawing-info  (db-interface/read-drawing-info drawing-id)
          rooms         {}; (db-interface/read-room-list drawing-id)
          session       (assoc session :drawing-id drawing-id)
          ]
          (log/info "Project ID:" project-id)
          (log/info "Project info" project-info)
          (log/info "Building ID:" building-id)
          (log/info "Building info" building-info)
          (log/info "Floor ID:" floor-id)
          (log/info "Floor info" floor-info)
          (log/info "Drawing ID:" drawing-id)
          (log/info "Drawing info" drawing-info)
          (log/info "Rooms" rooms)
          (if drawing-id
              (if drawing-info
                  (finish-processing request (html-renderer/render-drawing project-id building-id floor-id drawing-id project-info building-info floor-info drawing-info rooms) session)
                  (finish-processing request (html-renderer/render-error-page "Nebyl nalezen žádný výkres")))
              (finish-processing request (html-renderer/render-error-page "Nebyl vybrán žádný výkres")))))

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
        (rest-api/api-info-handler request)
        (condp = [method (get-api-command uri prefix)]
            [:get  ""]                 (rest-api/api-info-handler request)
            [:get  "info"]             (rest-api/info-handler request)
            [:get  "liveness"]         (rest-api/liveness-handler request)
            [:get  "readiness"]        (rest-api/readiness-handler request)
            [:get  "project-list"]     (rest-api/project-list-handler request uri)
            [:get  "project"]          (rest-api/project-handler request uri)
            [:get  "building"]         (rest-api/building-handler request uri)
            [:get  "floor"]            (rest-api/floor-handler request uri)
            [:get  "drawing"]          (rest-api/drawing-handler request uri)
            [:get  "all-drawings"]     (rest-api/all-drawings-handler request uri)
            [:put  "drawing-raw-data"] (rest-api/store-drawing-raw-data request)
            [:get  "raster-drawing"]   (drawing-renderer/raster-drawing request)
                                       (rest-api/unknown-endpoint request uri)
        )))

(defn uri->file-name
    [uri]
    (subs uri (inc (.indexOf uri "/"))))

(defn gui-call-handler
    "This function is used to handle all GUI calls. Three parameters are expected:
     data structure containing HTTP request, string with URI, and the HTTP method."
    [request uri method]
    (cond (.endsWith uri ".gif") (http-utils/return-file "www" (uri->file-name uri) "image/gif")
          (.endsWith uri ".png") (http-utils/return-file "www" (uri->file-name uri) "image/png")
          (.endsWith uri ".ico") (http-utils/return-file "www" (uri->file-name uri) "image/x-icon")
          (.endsWith uri ".css") (http-utils/return-file "www" (uri->file-name uri) "text/css")
          (.endsWith uri ".js")  (http-utils/return-file "www" (uri->file-name uri) "application/javascript")
          :else
        (condp = uri
            "/"                           (process-front-page request)
            "/settings"                   (process-settings-page request)
            "/store-settings"             (process-store-settings-page request)
            "/db-stats"                   (process-db-statistic-page request)
            "/drawings-stats"             (process-drawings-statistic-page request)
            "/project-list"               (process-project-list-page request)
            "/project-info"               (process-project-info-page request)
            "/project"                    (process-project-page request)
            "/building-info"              (process-building-info-page request)
            "/floor"                      (process-floor-page request)
            "/floor-info"                 (process-floor-info-page request)
            "/building"                   (process-building-page request)
            "/drawing"                    (process-drawing-page request)
            "/drawing-preview"            (process-drawing-preview-page request)
            "/vector-drawing"             (drawing-renderer/vector-drawing request)
            "/vector-drawing-as-json"     (drawing-renderer/vector-drawing-as-json request)
            "/raster-drawing"             (drawing-renderer/raster-drawing request)
            "/drawings-list"              (process-drawings-list request)
            "/json-list"                  (process-json-list request)
            )))

(defn handler
    "Handler that is called by Ring for all requests received from user(s)."
    [request]
    (log/info "request URI:   " (:uri request))
    (log/info "configuration: " (:configuration request))
    (let [uri             (:uri request)
          method          (:request-method request)
          api-prefix      (config/get-api-prefix request)
          api-full-prefix (config/get-api-full-prefix request)]
          (println uri)
        (cond (= uri api-prefix)            (rest-api/toplevel-handler request api-full-prefix)
              (= uri (str api-prefix "/"))  (rest-api/toplevel-handler request api-full-prefix)
              (startsWith uri api-prefix)   (api-call-handler request uri method api-full-prefix)
              :else                         (gui-call-handler request uri method))))
