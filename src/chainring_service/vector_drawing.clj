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

(ns chainring-service.vector-drawing
    "REST API handlers for returning vector drawings stored on disk.

    Author: Pavel Tisnovsky")


(require '[clojure.tools.logging :as log])

(require '[chainring-service.http-utils :as http-utils])


(defn send-drawing
    "Send drawing data to the client.
     Accepted parameters:
         request:   the structure with HTTP request data
         mime-type: the MIME type is usually set to text/plain or application/json
         extension: can be almost anything, though 'drw', 'json', and 'bin' is currently used"
    [request mime-type extension]
    (let [params       (:params request)
          drawing-id   (get params "drawing-id")
          drawing-name (get params "drawing-name")]
          (log/info "Drawing ID:" drawing-id)
          (cond drawing-id (try
                               (let [drawing-name (format (str "%05d." extension) (Integer/parseInt drawing-id))]
                                    (log/info "Drawing name:" drawing-name)
                                    (http-utils/return-file "drawings" drawing-name mime-type))
                               (catch Exception e
                                   (log/error e)))
                drawing-name (try
                               (log/info "Drawing name:" drawing-name)
                               (http-utils/return-file "drawings" drawing-name mime-type)
                               (catch Exception e
                                   (log/error e)))
                :else nil)))


(defn vector-drawing-as-drw
    "REST API handler for the /api/vector-drawing endpoint."
    [request]
    ; sent the vector drawing in a 'drw' format
    (send-drawing request "text/plain" "drw"))


(defn vector-drawing-as-json
    "REST API handler for the /api/vector-drawing-as-json endpoint."
    [request]
    ; sent the vector drawing in JSON format
    (send-drawing request "application/json" "json"))


(defn vector-drawing-as-binary
    "REST API handler for the /api/vector-drawing-as-binary endpoint."
    [request]
    ; sent the vector drawing in binary format
    (send-drawing request "application/octet-stream" "bin"))

