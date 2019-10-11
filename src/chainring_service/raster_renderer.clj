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

(ns chainring-service.raster-renderer
    "Namespace that contains functions to render drawings into raster images.

    Author: Pavel Tisnovsky")


(require '[clojure.string        :as str])

(require '[ring.util.response    :as http-response])
(require '[clojure.tools.logging :as log])
(require '[clojure.data.json     :as json])
(require '[clj-utils.utils       :as utils])

(require '[chainring-service.sap-interface    :as sap-interface])
(require '[chainring-service.http-utils       :as http-utils])
(require '[chainring-service.db-interface     :as db-interface])
(require '[chainring-service.drawings-storage :as drawings-storage])
(require '[chainring-service.drawings-cache   :as drawings-cache])

(def default-x-offset 50)

(import java.awt.Color)
(import java.awt.Font)
(import java.awt.BasicStroke)
(import java.awt.RenderingHints)
(import java.awt.Polygon)
(import java.awt.image.BufferedImage)
(import java.io.ByteArrayInputStream)
(import java.io.ByteArrayOutputStream)
(import javax.imageio.ImageIO)


(defn proper-scale?
    "Predicate if the given item contains expected width and height attributes."
    [item width height]
    (and (= (:width item)  width)
         (= (:height item) height)))


(defn get-scale-from-scales
    "Return scale set up for given width and height values."
    [scales width height]
    (first (filter #(proper-scale? % width height) scales)))


(defn get-scale
    "Return scale set up for given width and height values."
    [data width height]
    (get-scale-from-scales (get data :scales) width height))


(defn transform
    "Transform x or y coordinate using provided scale and offset."
    [coordinate scale offset after-offset center]
    ;(int (+ center after-offset (* scale (+ coordinate offset (- center))))))
    (int (+ center after-offset (* scale (+ coordinate offset (- center))))))
    ;(int (+ after-offset (* scale (+ coordinate offset)))))


; TODO: background color to be read from configuration
; TODO: foreground color to be read from configuration
(defn setup-graphics-context
    "Set up graphics context."
    [image gc width height]
    (let [rh (new RenderingHints RenderingHints/KEY_RENDERING RenderingHints/VALUE_RENDER_QUALITY)]
        (.put rh RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
        (.put rh RenderingHints/KEY_ANTIALIASING      RenderingHints/VALUE_ANTIALIAS_ON)
        (.setRenderingHints gc rh))
    (let [font (new Font "Helvetica" Font/PLAIN 8)]
        (.setFont gc font))
    (.setBackground gc (new Color 1.0 1.0 1.0))
    (.clearRect gc 0 0 width height)
    (.setColor gc Color/BLACK)
    (.drawRect gc 0 0 (dec width) (dec height)))


(defn draw-line
    "Draw a line entity onto drawing canvas."
    [gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center]
    (let [x1 (transform (:x1 entity) scale x-offset user-x-offset h-center)
          y1 (transform (:y1 entity) scale y-offset user-y-offset v-center)
          x2 (transform (:x2 entity) scale x-offset user-x-offset h-center)
          y2 (transform (:y2 entity) scale y-offset user-y-offset v-center)]
          (.drawLine gc x1 y1 x2 y2)))


(defn draw-arc
    "Draw an arc entity onto drawing canvas."
    [gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center]
    (let [x (transform (:x entity) scale x-offset user-x-offset h-center)
          y (transform (:y entity) scale y-offset user-y-offset v-center)
          r (int (* scale (:r entity)))
          a1 (:a1 entity)
          a2 (:a2 entity)
          delta (int (- a2 a1))
          extent (if (neg? delta) (+ delta 360) delta)]
          (.drawArc gc (- x r) (- y r) (* r 2) (*  r 2) a1 extent)))


(defn draw-circle
    "Draw a circle entity onto drawing canvas."
    [gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center]
    (let [x (transform (:x entity) scale x-offset user-x-offset h-center)
          y (transform (:y entity) scale y-offset user-y-offset v-center)
          r (int (* scale (:r entity)))]
          (.drawOval gc (- x r) (- y r) (* r 2) (*  r 2))))


(defn draw-text
    "Draw a text entity onto drawing canvas."
    [gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center]
    (let [x (transform (:x entity) scale x-offset user-x-offset h-center)
          y (transform (:y entity) scale y-offset user-y-offset v-center)
          t (:text entity)]
          (.drawString gc t x y)))


(defn draw-entities
    "Draw all entities onto drawing canvas."
    [gc entities scale x-offset y-offset user-x-offset user-y-offset h-center v-center show-dimensions]
    (.setColor gc Color/BLACK)
    (doseq [entity entities]
        (if (or show-dimensions (not= "koty" (clojure.string/lower-case (str (:layer entity)))))
            (condp = (:T entity) 
                "L" (draw-line   gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center)
                "C" (draw-circle gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center) 
                "A" (draw-arc    gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center)
                "T" (draw-text   gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center)
                    nil
        ))))


(defn draw-entities-from-binary-file
    "Draw all entities read from binary file."
    [gc fin entity-count scale x-offset y-offset user-x-offset user-y-offset h-center v-center]
    (.setColor gc Color/BLACK)
    (doseq [i (range entity-count)]
        (let [entity (drawings-storage/read-entity-from-binary fin)]
            (condp = (:T entity) 
                "L" (draw-line   gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center)
                "C" (draw-circle gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center) 
                "A" (draw-arc    gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center)
                "T" (draw-text   gc entity scale x-offset y-offset user-x-offset user-y-offset h-center v-center)
                    nil
            ))))


(defn draw-room-background
    "Draw background of highlighted room."
    [gc xpoints ypoints background-color]
    (.setColor gc background-color)
    (.fillPolygon gc (int-array xpoints)
                     (int-array ypoints)
                     (count xpoints)))


(defn draw-room-contour
    "Draw contour of highlighted room."
    [gc xpoints ypoints foreground-color]
    (.setColor gc foreground-color)
    (.drawPolygon gc (int-array xpoints)
                     (int-array ypoints)
                     (count xpoints)))


(defn draw-selected-room
    "Draw background and contour of selected room."
    [gc xpoints ypoints]
    (draw-room-background gc xpoints ypoints (new Color 1.0 1.0 0.5 0.5))
    (draw-room-contour    gc xpoints ypoints Color/RED))


(defn draw-highlighted-room
    "Draw background and contour of highlighted room."
    [gc xpoints ypoints aoid room-colors]
    (let [colors (get room-colors aoid)
          foreground-color (:foreground colors)
          background-color (:background colors)]
          (draw-room-background gc xpoints ypoints background-color)
          (draw-room-contour    gc xpoints ypoints foreground-color)))


(defn draw-regular-room
    "Draw regular room onto the canvas."
    [gc xpoints ypoints]
    (draw-room-contour gc xpoints ypoints Color/BLUE))


(defn coords-in-polygon
    "Test if specified coordinates lies in the polygon."
    [xpoints ypoints coordsx coordsy]
    (if (and coordsx coordsy)
        (let [polygon (new Polygon (int-array xpoints) (int-array ypoints) (count xpoints))]
             (.contains polygon (double coordsx) (double coordsy)))))


(defn selected-room?
    "Check if the room was selected by user."
    [aoid selected]
    (= aoid selected))


(defn highlighted-room?
    "Check if the room was highlighted."
    [aoid room-colors]
    (get room-colors aoid nil))


(defn draw-room
    "Draw room that is passed via the 'room' parameter."
    [gc room scale x-offset y-offset user-x-offset user-y-offset selected room-colors h-center v-center]
    (let [polygon (:polygon room)
          aoid    (:room_id room)
          xpoints (map first polygon)
          ypoints (map second polygon)
          transformed-xpoints (map #(transform % scale x-offset user-x-offset h-center) xpoints)
          transformed-ypoints (map #(transform % scale y-offset user-y-offset v-center) ypoints)]
          (if (seq xpoints)
              (cond
                  (selected-room? aoid selected)
                      (draw-selected-room gc transformed-xpoints transformed-ypoints)
                  (highlighted-room? aoid room-colors)
                      (draw-highlighted-room gc transformed-xpoints transformed-ypoints aoid room-colors)
                  :else (draw-regular-room gc transformed-xpoints transformed-ypoints)))))


(defn draw-rooms
    "Draw all rooms that are passed via the 'rooms' parameter."
    [gc rooms scale x-offset y-offset user-x-offset user-y-offset selected room-colors h-center v-center]
    (.setStroke gc (new BasicStroke 2))
    (doseq [room rooms]
        (draw-room gc room scale x-offset y-offset user-x-offset user-y-offset selected room-colors h-center v-center)))


(defn draw-rooms-from-binary
    "Draw rooms, data is read from the binary file."
    [gc fin rooms-count scale x-offset y-offset user-x-offset user-y-offset selected room-colors h-center v-center]
    (.setStroke gc (new BasicStroke 2))
    (doseq [i (range rooms-count)]
        (let [room (drawings-storage/read-room-from-binary fin)]
            (draw-room gc room scale x-offset y-offset user-x-offset user-y-offset selected room-colors h-center v-center))))


(defn draw-grid
    "Draw a regular grid onto the canvas."
    [gc width height grid-size grid-color]
    (.setColor gc grid-color)
    (.setStroke gc (new BasicStroke 1))
    (doseq [y (range 0 (inc height) grid-size)]
        (doseq [x (range 0 (inc width) grid-size)]
            (.drawLine gc x y x y))))
            ;(.drawLine gc (dec x) y (inc x) y)
            ;(.drawLine gc x (dec y) x (inc y)))))


(defn draw-boundary
    "Draw drawing boundary onto the canvas."
    [gc bounds scale x-offset y-offset user-x-offset user-y-offset boundary-color h-center v-center]
    (.setColor gc boundary-color)
    (.setStroke gc (new BasicStroke 1))
    (let [x1 (transform (:xmin bounds) scale x-offset user-x-offset h-center)
          y1 (transform (:ymin bounds) scale y-offset user-y-offset v-center)
          x2 (transform (:xmax bounds) scale x-offset user-x-offset h-center)
          y2 (transform (:ymax bounds) scale y-offset user-y-offset v-center)]
          (.drawRect gc x1 y1 (- x2 x1) (- y2 y1))
          (.drawRect gc (+ x1 2) (+ y1 2) (- x2 x1 4) (- y2 y1 4))))


(defn draw-selection-point
    "Draw selection point onto the canvas."
    [gc x y blip-size blip-color]
    (when (and x y)
          (.setStroke gc (new BasicStroke 1))
          (.setColor gc blip-color)
          (.drawLine gc (- x blip-size) y (+ x blip-size) y)
          (.drawLine gc x (- y blip-size) x (+ y blip-size))))


(defn draw-timestamp
    "Draw a timestamp onto the canvas."
    [gc timestamp width height]
    (.setColor gc (new Color 50 50 50))
    (.drawString gc timestamp (- width 140) (- height 6)))


(defn drawing-full-name
    "Construct filename with drawing from either drawing id or drawing name."
    [drawing-id drawing-name]
    (if drawing-id
        (format (str "drawings/%s.json") drawing-id)
        (if drawing-name
            (str "drawings/" drawing-name))))


(defn drawing-full-name-binary
    "Construct filename with drawing stored in binary file from either drawing id or drawing name."
    [drawing-id drawing-name]
    (if drawing-id
        (format (str "drawings/%05d.bin") (Integer/parseInt drawing-id))
        (if drawing-name
            (str "drawings/" drawing-name))))


(defn read-drawing-from-json
    "Read drawing data from the JSON file."
    ( [filename]
        (let [start-time (System/currentTimeMillis)
              data       (json/read-str (slurp filename) :key-fn clojure.core/keyword)
              end-time   (System/currentTimeMillis)]
              (log/info "JSON loading time (ms):" (- end-time start-time))
              data))
    ( [filename message]
        (log/info message)
        (read-drawing-from-json filename)))


(defn prepare-data-stream
    "Prepare data stream with binary drawing data."
    [filename]
    (let [fis (new java.io.FileInputStream filename)]
        (new java.io.DataInputStream fis)))


(defn draw-into-image-from-binary-data
    "Draw the drawing read from binary file onto the raster image."
    [image drawing-id drawing-name width height user-x-offset user-y-offset user-scale
     selected room-colors coordsx coordsy show-grid show-boundary show-blips
     debug configuration]
    (if-let [full-name        (drawing-full-name-binary drawing-id drawing-name)]
        (let [fin             (prepare-data-stream full-name)
              gc              (.createGraphics image)
              grid-size       (-> configuration :renderer :grid-size)
              grid-rgb        (-> configuration :renderer :grid-color)
              grid-color      (utils/rgb->Color grid-rgb)
              boundary-rgb    (-> configuration :renderer :boundary-color)
              boundary-color  (utils/rgb->Color boundary-rgb)
              blip-size       (-> configuration :renderer :blip-size)
              blip-rgb        (-> configuration :renderer :blip-color)
              blip-color      (utils/rgb->Color blip-rgb)]
            (try
                (let [[magic-number file-version data-version] (drawings-storage/read-binary-header fin)
                      [created-ms created]                     (drawings-storage/read-created-time fin)
                      [entity-count rooms-count scales-count]  (drawings-storage/read-counters fin)
                      bounds                                   (drawings-storage/read-bounds fin)
                      scales                                   (drawings-storage/read-scales fin scales-count)
                      scale-info (get-scale-from-scales scales width height)
                      h-center        (/ width 2)
                      v-center        (/ height 2)
                      x-offset   (:xoffset scale-info)
                      y-offset   (:yoffset scale-info)
                      scale      (* (:scale scale-info) user-scale)]
                    (assert (= magic-number 0x6502))
                    (assert (= file-version 1))
                    (assert (= data-version 1))
                    (when debug
                          (log/info "full drawing name" full-name)
                          (log/info "magic number " (Integer/toString magic-number 16))
                          (log/info "file version" file-version)
                          (log/info "data version" data-version)
                          (log/info "created (ms)" created-ms)
                          (log/info "created (ms)" (.toString created))
                          (log/info "entities" entity-count)
                          (log/info "rooms"    rooms-count)
                          (log/info "scales"   scales-count)
                          (log/info "bounds"   bounds)
                          (log/info "x-offset" x-offset)
                          (log/info "y-offset" y-offset)
                          (log/info "scale:" scale)
                          (log/info "scale-info:" scale-info)
                          (log/info "width" width)
                          (log/info "height" height)
                          (doseq [scale scales]
                              (log/info "scale" scale)))
                    (let [start-time (System/currentTimeMillis)]
                        (setup-graphics-context image gc width height)
                        (log/info "gc:" gc)
                        (draw-entities-from-binary-file gc fin entity-count scale x-offset y-offset user-x-offset user-y-offset h-center v-center)
                        (draw-rooms-from-binary gc fin rooms-count scale x-offset y-offset user-x-offset user-y-offset selected room-colors h-center v-center)
                        (if (or debug show-blips)
                            (draw-selection-point gc coordsx coordsy blip-size blip-color))
                        (log/info "Rasterization time (ms):" (- (System/currentTimeMillis) start-time))
                    ))
                (catch Throwable e
                    (log/error e)
                    (.close fin)))
        )
    ))


(defn get-drawing-data
    "Retrieve data with drawing with possible use of drawing cache."
    [drawing-id drawing-name use-memory-cache]
    (when-let [full-name (drawing-full-name drawing-id drawing-name)]
        (log/info "full drawing name:" full-name)
        (log/info "memory cache: " use-memory-cache)
        (if (and drawing-id use-memory-cache)
            (let [data (or (drawings-cache/fetch drawing-id) (read-drawing-from-json full-name "Cache miss, must read JSON"))]
                 (drawings-cache/store drawing-id data)
                 data)
            (read-drawing-from-json full-name "Forced read from JSON"))))


(defn offset+scale
    "Apply linear transformation: offset + scale to x and y coordinates."
    [data width height user-scale]
    (let [scale-info (get-scale data width height)
          x-offset   (:xoffset scale-info)
          y-offset   (:yoffset scale-info)
          scale      (* (:scale scale-info) user-scale)]
          [x-offset y-offset scale]))


(defn draw-into-image
    "Draw the drawing onto the raster image."
    [image drawing-id drawing-name width height user-x-offset user-y-offset user-scale
     selected room-colors coordsx coordsy use-memory-cache
     show-grid show-boundary show-blips show-dimensions
     debug configuration timestamp]
    (let [data (get-drawing-data drawing-id drawing-name use-memory-cache)]
        (if data
        (let [[x-offset y-offset scale] (offset+scale data width height user-scale)
              h-center        (/ width 2)
              v-center        (/ height 2)
              entities        (:entities data)
              rooms           (:rooms data)
              bounds          (:bounds data)
              gc              (.createGraphics image)
              grid-size       (-> configuration :renderer :grid-size)
              grid-rgb        (-> configuration :renderer :grid-color)
              grid-color      (utils/rgb->Color grid-rgb)
              boundary-rgb    (-> configuration :renderer :boundary-color)
              boundary-color  (utils/rgb->Color boundary-rgb)
              blip-size       (-> configuration :renderer :blip-size)
              blip-rgb        (-> configuration :renderer :blip-color)
              blip-color      (utils/rgb->Color blip-rgb)]
            (log/info "width x height" width height)
            (log/info "offset" x-offset y-offset)
            (log/info "scale:" scale)
            (log/info "entities:" (count entities))
            (log/info "rooms" (count rooms))
            (log/info "bounds" bounds)
            (log/info "selected" selected)
            (log/info "clicked" coordsx coordsy)
            (log/info "grid" show-grid grid-size grid-color)
            (log/info "boundary" show-boundary)
            (log/info "blip" show-blips blip-size blip-rgb)
            (log/info "dimensions" show-dimensions)
            (log/info "debug" debug)
            (let [start-time (System/currentTimeMillis)]
                (setup-graphics-context image gc width height)
                (log/info "gc:" gc)
                (if show-grid
                    (draw-grid gc width height grid-size grid-color))
                (draw-entities gc entities scale x-offset y-offset user-x-offset user-y-offset h-center v-center show-dimensions)
                (draw-rooms gc rooms scale x-offset y-offset user-x-offset user-y-offset selected room-colors h-center v-center)
                (if show-boundary
                    (draw-boundary gc bounds scale x-offset y-offset user-x-offset user-y-offset boundary-color h-center v-center))
                (if (or debug show-blips)
                    (draw-selection-point gc coordsx coordsy blip-size blip-color))
                (draw-timestamp gc timestamp width height)
                (log/info "Rasterization time (ms):" (- (System/currentTimeMillis) start-time)))
        )
    )))


(defn aoid+selected
    "Return room that is selected by user (x,y) or by AOID (SAP)."
    [rooms scale x-offset user-x-offset y-offset user-y-offset
     coordsx coordsy h-center v-center]
     (for [room rooms]
         (let [polygon (:polygon room)
               aoid    (:room_id room)
               xpoints (map first polygon)
               ypoints (map second polygon)
               transformed-xpoints (map #(transform % scale x-offset user-x-offset h-center) xpoints)
               transformed-ypoints (map #(transform % scale y-offset user-y-offset v-center) ypoints)]
               (if (seq xpoints)
                   {:aoid aoid
                    :selected (coords-in-polygon transformed-xpoints transformed-ypoints coordsx coordsy)}
                   {:aoid aoid
                    :selected nil}))))


(defn find-room
    "Find room that is selected by user (x,y) or by AOID (SAP)."
    [drawing-id drawing-name width height
     user-x-offset user-y-offset user-scale coordsx coordsy use-memory-cache
     h-center v-center]
    (let [data (get-drawing-data drawing-id drawing-name use-memory-cache)]
        (if data
            (let [[x-offset y-offset scale] (offset+scale data width height user-scale)
                  rooms      (:rooms data)
                  aoids      (aoid+selected rooms scale x-offset user-x-offset y-offset user-y-offset coordsx coordsy h-center v-center)]
                  ; output value
                  (->> aoids
                      (filter #(:selected %))
                      first
                      :aoid)))))


(def color1  (new Color 240 100 00))
(def color2  (new Color 200 100 40))
(def color3  (new Color 160 100 80))
(def color4  (new Color 120 100 120))
(def color5  (new Color  80 100 160))
(def color6  (new Color  40 100 200))
(def color7  (new Color  00 100 240))
(def color8  (new Color 100  40 100))
(def color9  (new Color 100  80 100))
(def color10 (new Color 100 120 100))

(def color1-alpha (new Color 240 100 00 127))
(def color2-alpha (new Color 200 100 40 127))
(def color3-alpha (new Color 160 100 80 127))
(def color4-alpha (new Color 120 100 120 127))
(def color5-alpha (new Color  80 100 160 127))
(def color6-alpha (new Color  40 100 200 127))
(def color7-alpha (new Color  00 100 240 127))
(def color8-alpha (new Color 100  40 100 127))
(def color9-alpha (new Color 100  80 100 127))
(def color10-alpha (new Color 100 120 100 127))


(defn fg-color
    "Construct foreground color from R, G, B."
    [r g b]
    (new Color r g b))


(defn bg-color
    "Construct background color from R, G, B."
    [r g b]
    (new Color r g b 127))


(def occupation-colors [
     {:foreground (fg-color 70 70 70) :background (bg-color 100 100 100)} ; Nepronajímatelné
     {:foreground (fg-color 0 200 0)  :background (bg-color 70 70 200)}   ; Interní
     {:foreground (fg-color 200 0 0)  :background (bg-color 240 70 70)}   ; Pronajímatelné - obsazené
     {:foreground (fg-color 0 0 200)  :background (bg-color 70 240 70)}   ; Pronajímatelné - neobsazené
])

(def cleanup-colors [
    {:foreground (new Color 150 150 100) :background (new Color 150,150,150,127)}   ; 100601 Četnost úklidu-bez úklidu
    {:foreground (new Color 250 150 150) :background (new Color   0, 50,  0,127)}   ; 100602 Četnost úklidu-1xtýdně
    {:foreground (new Color 100 150 200) :background (new Color   0,150,  0,127)}   ; 100603 Četnost úklidu-2xtýdně
    {:foreground (new Color 200 140 200) :background (new Color   0,250,  0,127)}   ; 100604 Četnost úklidu-3xtýdně
    {:foreground (new Color 100 200 200) :background (new Color  50,  0,  0,127)}   ; 100605 Četnost úklidu-4xtýdně
    {:foreground (new Color 200 200 100) :background (new Color 150,  0,  0,127)}   ; 100606 Četnost úklidu-5xtýdně
    {:foreground (new Color 250 50 50)   :background (new Color 250,  0,  0,127)}   ; 100607 Četnost úklidu-6x týdně
    {:foreground (new Color 250 0 0)     :background (new Color   0,  0, 50,127)}   ; 100608 Četnost úklidu-7x týdně
    {:foreground (new Color 150 150 100) :background (new Color   0,  0,150,127)}   ; 100609 Četnost úklidu-1x 14dní
    {:foreground (new Color 250 150 150) :background (new Color   0,  0,250,127)}   ; 100610 Četnost úklidu-1x měsíc
    {:foreground (new Color 100 150 200) :background (new Color   0,250,250,127)}   ; 100611 Četnost úklidu-Ostatní
])

(def room-colors [
    {:foreground (new Color 150 150 100) :background (new Color   0,  0, 90,127)}
    {:foreground (new Color 250 150 150) :background (new Color   0,150,  0,127)}
    {:foreground (new Color 100 150 200) :background (new Color   0,150,150,127)}
    {:foreground (new Color 200 140 200) :background (new Color 150,  0,  0,127)}
    {:foreground (new Color 100 200 200) :background (new Color 150,  0,150,127)}
    {:foreground (new Color 200 200 100) :background (new Color 150,150,  0,127)}
    {:foreground (new Color 250 50 50)   :background (new Color 150,150,150,127)}
    {:foreground (new Color 250 0 0)     :background (new Color  60, 60,255,127)}
    {:foreground (new Color 150 150 100) :background (new Color   0,250,  0,127)}
    {:foreground (new Color 250 150 150) :background (new Color   0,250,250,127)}
    {:foreground (new Color 100 150 200) :background (new Color 250,  0,  0,127)}
    {:foreground (new Color 200 140 200) :background (new Color 250,  0,250,127)}
    {:foreground (new Color 100 200 200) :background (new Color 250,250,  0,127)}
    {:foreground (new Color 200 200 100) :background (new Color 250,250,250,127)}
    {:foreground (new Color 250 50 50)   :background (new Color 150,150,150,127)}
    {:foreground (new Color 250 0 0)     :background (new Color  50, 50, 50,127)}
])

(def purpose-colors [
    {:foreground (new Color 150 150 100) :background (new Color   0,  0,150,127)}
    {:foreground (new Color 250 150 150) :background (new Color   0,150,  0,127)}
    {:foreground (new Color 100 150 200) :background (new Color   0,150,150,127)}
    {:foreground (new Color 200 140 200) :background (new Color 150,  0,  0,127)}
    {:foreground (new Color 100 200 200) :background (new Color 150,  0,150,127)}
    {:foreground (new Color 200 200 100) :background (new Color 150,150,  0,127)}
    {:foreground (new Color 250 50 50)   :background (new Color 150,150,150,127)}
    {:foreground (new Color 250 0 0)     :background (new Color   0,  0,250,127)}
    {:foreground (new Color 150 150 100) :background (new Color   0,250,  0,127)}
    {:foreground (new Color 250 150 150) :background (new Color   0,250,250,127)}
    {:foreground (new Color 100 150 200) :background (new Color 250,  0,  0,127)}
    {:foreground (new Color 200 140 200) :background (new Color 250,  0,250,127)}
    {:foreground (new Color 100 200 200) :background (new Color 250,250,  0,127)}
    {:foreground (new Color 200 200 100) :background (new Color 250,250,250,127)}
    {:foreground (new Color 250 50 50)   :background (new Color 150,150,150,127)}
    {:foreground (new Color 250 0 0)     :background (new Color  50, 50, 50,127)}
    {:foreground (new Color 150 200 200) :background (new Color 250,250,  0,127)}
    {:foreground (new Color 200 200 150) :background (new Color 250,250,250,127)}
    {:foreground (new Color 250 150 50)  :background (new Color 150,150,150,127)}
    {:foreground (new Color 250 0 100)   :background (new Color  50, 50, 50,127)}
])



(def contract-colors [
    {:foreground (fg-color 0 0 200),
     :background (bg-color 70 70 240)}
    {:foreground (fg-color 100 100 0)
     :background (bg-color 220 220 70)}
])


(def contract-types    [1 2])
(def occupation-types  [1 2 3 4])
(def room-types        [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16])
(def purpose-types     [1 2 3 4 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20])
(def cleanup-types     [100601 100602 100603 100604 100605 100606 100607 100608 100609 100610 100611])


(defn attribute-color
    "Compute color for specified room attribute."
    [room-attribute values-to-show values colors]
    (print room-attribute " ")
    (let [i (.indexOf values room-attribute)]
        (if (>= i 0)
            (if (get values-to-show i)
                (get colors i {:foreground Color/GRAY :background (new Color 255 255 255 127)})))))


(defn compute-room-color-static-values
    "Compute room color for specified room attribute with static values."
    [highlight-group room-attribute-numeric-code values-to-show]
    (condp = highlight-group
        :typ        (attribute-color room-attribute-numeric-code values-to-show room-types       room-colors)
        :OB         (attribute-color room-attribute-numeric-code values-to-show occupation-types occupation-colors)
        :DS         (attribute-color room-attribute-numeric-code values-to-show contract-types   contract-colors)
        :UK         (attribute-color room-attribute-numeric-code values-to-show cleanup-types    cleanup-colors)
        :UP         (attribute-color room-attribute-numeric-code values-to-show purpose-types    purpose-colors)
        ;:obsazenost (attribute-color room-attribute-numeric-code values-to-show occupation-types occupation-colors)
        ;:smlouva    (attribute-color room-attribute-numeric-code values-to-show contract-types   contract-colors)
        ;:uklid      (attribute-color room-attribute-numeric-code values-to-show cleanup-types    cleanup-colors)
        nil))


(def palette [
    {:foreground Color/GRAY :background (bg-color   0   0 150)}
    {:foreground Color/GRAY :background (bg-color   0 150   0)}
    {:foreground Color/GRAY :background (bg-color   0 150 150)}
    {:foreground Color/GRAY :background (bg-color 150   0   0)}
    {:foreground Color/GRAY :background (bg-color 150   0 150)}
    {:foreground Color/GRAY :background (bg-color 150 150   0)}
    {:foreground Color/GRAY :background (bg-color 150 150 150)}
    {:foreground Color/GRAY :background (bg-color   0   0 250)}
    {:foreground Color/GRAY :background (bg-color   0 250   0)}
    {:foreground Color/GRAY :background (bg-color   0 250 250)}
    {:foreground Color/GRAY :background (bg-color 250   0   0)}
    {:foreground Color/GRAY :background (bg-color 250   0 250)}
    {:foreground Color/GRAY :background (bg-color 250 250   0)}
    {:foreground Color/GRAY :background (bg-color 250 250 250)}
    {:foreground Color/GRAY :background (bg-color 150 150 150)}
    {:foreground Color/GRAY :background (bg-color  50  50  50)}
    {:foreground Color/GRAY :background (bg-color 250 250   0)}
    {:foreground Color/GRAY :background (bg-color 250 250 250)}
    {:foreground Color/GRAY :background (bg-color 150 150 150)}
    {:foreground Color/GRAY :background (bg-color  50  50  50)}
])

(defn compute-room-color-list-of-values
    "Compute room color for specified room attribute with list of values."
    [all-room-attributes room-attribute values-to-show]
    (if (seq room-attribute)
        (let [i (.indexOf all-room-attributes room-attribute)
              im (mod i (count palette))]
              (println room-attribute)
              (println im)
              (if (get values-to-show im)
                  (nth palette im)))))

;
; input:
;
;{1.A1.1MP.500 {:value voda1,voda2,voda3, :key voda1,voda2,voda3},
; 1.A1.1MP.514 {:value voda5,voda6, :key voda5,voda6},
; 1.A1.1MP.502 {:value voda1,voda2,voda6, :key voda1,voda2,voda6},
; 1.A1.1MP.505 {:value voda1, :key voda1},
;

(defn get-all-room-attributes
    "Retrieve all room attributes in sorted and unique format."
    [room-attrs radio-buttons]
    (println "")
    (println "Attributes")
    (println room-attrs)
    (if radio-buttons
        (->> room-attrs
             vals
             (map #(:value %))
             (map clojure.string/trim)
             (map #(clojure.string/split % #","))
             flatten
             (map clojure.string/trim)
             distinct
             (filter #(seq %))
             sort
             (into []))
        (->> room-attrs
             vals
             (map #(:value %))
             (filter #(seq %))
             distinct
             sort
             (into []))))


(defn room-with-attribute
    "Check if the room has the specified attribute."
    [room-attrs attribute]
    (let [room-id    (key room-attrs)
          values-str (:value (val room-attrs))
          values     (into #{} (for [v (clojure.string/split values-str #",")] (clojure.string/trim v)))]
          (contains? values attribute)))


(defn compute-room-colors-radio-buttons
    "Compute room colors for radio-buttons-based attributes."
    [all-room-attributes room-attrs selected-radio-button]
    (if (seq selected-radio-button)
        (try
            (let [i     (utils/parse-int selected-radio-button)
                  im    (mod i (count palette))
                  color (nth palette im)
                  attribute (nth all-room-attributes i)
                  rooms (filter #(room-with-attribute % attribute) room-attrs)]
                  (into {} (for [room rooms] [(key room) color])))
             (catch Exception e
                 {})) ; fallback
        {}))


(defn compute-room-colors-no-radio-buttons
    "Compute room colors for normal attributes."
    [all-room-attributes highlight-group room-attrs values-to-show]
    (if (some #{highlight-group} [:typ :UP :UK :DS :OB :uklid :obsazenost :smlouva])
        (into {} (for [room room-attrs] [(key room) (compute-room-color-static-values highlight-group (:key (val room)) values-to-show)]))
        (into {} (for [room room-attrs] [(key room) (compute-room-color-list-of-values all-room-attributes (:value (val room)) values-to-show)]))))


(defn compute-room-colors
    "Compute room colors."
    [all-room-attributes highlight-group room-attrs values-to-show selected-radio-button radio-buttons?]
    ;(println "vvvvvvvvvvvvvvvvvvvvvv")
    (println "all room attributes:" all-room-attributes)
    (println "radio buttons: " radio-buttons?)
    (println "radio button: " selected-radio-button)
    ;(println "^^^^^^^^^^^^^^^^^^^^^^")
    (if radio-buttons?
        (compute-room-colors-radio-buttons all-room-attributes room-attrs selected-radio-button)
        (compute-room-colors-no-radio-buttons all-room-attributes highlight-group room-attrs values-to-show)))


(defn use-binary-rendering?
    "Check whether we can use rendering with date read from binary file."
    [use-binary? drawing-name]
    ; if drawing-name is set, use this name to decide
    ; otherwise use the settings 'use-binary?'
    (if drawing-name
        (.endsWith drawing-name ".bin")
        use-binary?))


(defn room->aoid+attribute-
    "Retrieve AOID+attributes for the specified room."
    [room]
    (let [splitted (str/split room #"\|")]
        (if (== (count splitted) 3)
            (try
                 [(first splitted) {:value (second splitted) :key (utils/parse-int (utils/third splitted))}]
             (catch NumberFormatException e
                 [(first splitted) {:value (second splitted) :key (utils/third splitted)}]))
            nil)))


(defn room->aoid+attribute
    "Retrieve AOID+attributes for the specified room."
    [room]
    (try
         [(:AOID room) {:value (:value room) :key (utils/parse-int (:key room))}]
     (catch NumberFormatException e
         [(:AOID room) {:value (:value room) :key (:key room)}])))


(defn decode-attrs
    "Decode room attributes."
    [rooms]
    (try
        (if rooms
            (let [room+attr (str/split rooms #"_")]
                (into {} (for [r room+attr] (room->aoid+attribute r)))))
        (catch Exception e
            (log/error e))))


(defn decode-from-sap
    "Decode room attributes read from SAP."
    [rooms]
    (try
        (if rooms
            (into {} (for [r rooms] (room->aoid+attribute r))))
        (catch Exception e
            (log/error e))))


(defn read-values-to-show
    "Read values that needs to be shown from HTTP cookies."
    [cookies]
    (let [ks (->> cookies
                  keys
                  (filter #(.startsWith % "value_"))
                  sort)
          result (zipmap (for [k ks] (utils/parse-int (subs k (count "value_"))))
                         (for [k ks] (= "1" (-> (get cookies k) :value))))]
        result))


(defn perform-raster-drawing
    "Perform the rendering with highlighted rooms, grid, bounds, timestamp, etc."
    [request]
    (let [params                (:params request)
          configuration         (:configuration request)
          cookies               (:cookies request)
          ignore-type           (get params "ignore-type")
          values-to-show        (read-values-to-show cookies)
          selected-radio-button (-> (get cookies "radio_value") :value)
          highlight-group       (-> (get cookies "attribute") :value keyword)
          attribute             (-> (get cookies "attribute") :value)
          floor-id              (-> (get cookies "floor_id") :value)
          valid-from            (-> (get cookies "valid_from") :value)
          radio-buttons?        (some #{highlight-group} [:MT :MV :ME])
          room-attrs            (if (= ignore-type "true")
                                    []
                                    (-> (sap-interface/call-sap-interface request "read-rooms-attribute" floor-id valid-from attribute) decode-from-sap))
          ;room-attrs            (-> (get cookies "rooms") :value decode-attrs)
          all-room-attributes   (get-all-room-attributes room-attrs radio-buttons?)
          room-colors           (if (= ignore-type "true")
                                    nil
                                    (compute-room-colors all-room-attributes highlight-group room-attrs values-to-show selected-radio-button radio-buttons?))
          use-binary?         (-> configuration :drawings :use-binary)
          use-memory-cache    (-> configuration :drawings :use-memory-cache)
          drawing-id          (get params "drawing-id")
          drawing-name        (get params "drawing-name")
          width               (get params "width" 800)
          height              (get params "height" 600)
          user-x-offset       (utils/parse-int (get params "x-offset" "0"))
          user-y-offset       (utils/parse-int (get params "y-offset" "0"))
          user-scale          (utils/parse-float (get params "scale" "1.0"))
          selected            (get params "selected")
          coordsx             (get params "coordsx")
          coordsy             (get params "coordsy")
          ; make sure that only 'true' is converted into truth value, nil/false otherwise
          show-grid           (-> (get params "grid" "false")       utils/parse-boolean)
          show-boundary       (-> (get params "boundary" "false")   utils/parse-boolean)
          show-blips          (-> (get params "blip" "false")       utils/parse-boolean)
          show-dimensions     (-> (get params "dimensions" "false") utils/parse-boolean)
          coordsx-f           (if coordsx (Double/parseDouble coordsx))
          coordsy-f           (if coordsx (Double/parseDouble coordsy))
          debug               (get params "debug" nil)
          timestamp           (.toString (new java.util.Date))
          image               (new BufferedImage width height BufferedImage/TYPE_INT_RGB)
          image-output-stream (ByteArrayOutputStream.)]
          (println "----------")
          (println attribute)
          (println floor-id)
          (println valid-from)
          (println room-attrs)
          (println cookies)
          (try
              (if (use-binary-rendering? use-binary? drawing-name)
                  (draw-into-image-from-binary-data image drawing-id drawing-name
                                   width height
                                   (- user-x-offset default-x-offset) user-y-offset user-scale
                                   selected room-colors coordsx-f coordsy-f
                                   show-grid show-boundary show-blips show-dimensions
                                   debug configuration timestamp)
                  (draw-into-image image drawing-id drawing-name
                                   width height
                                   (- user-x-offset default-x-offset) user-y-offset user-scale
                                   selected room-colors coordsx-f coordsy-f
                                   use-memory-cache
                                   show-grid show-boundary show-blips show-dimensions
                                   debug configuration timestamp))
              (catch Exception e
                  (log/error "error during drawing!" e)))
          ; serialize image into output stream
          (ImageIO/write image "png" image-output-stream)
          (new ByteArrayInputStream (.toByteArray image-output-stream))))


(defn perform-find-room
    "Try to find room on (coordsx, coordsy) on drawing."
    [request]
    (let [params              (:params request)
          configuration       (:configuration request)
          use-binary?         (-> configuration :drawings :use-binary)
          use-memory-cache    (-> configuration :drawings :use-memory-cache)
          drawing-id          (get params "drawing-id")
          drawing-name        (get params "drawing-name")
          width               (get params "width" 800)
          height              (get params "height" 600)
          h-center            (/ width 2)
          v-center            (/ height 2)
          user-x-offset       (utils/parse-int (get params "x-offset" "0"))
          user-y-offset       (utils/parse-int (get params "y-offset" "0"))
          user-scale          (utils/parse-float (get params "scale" "1.0"))
          coordsx             (get params "coordsx")
          coordsy             (get params "coordsy")
          coordsx-f           (if coordsx (Double/parseDouble coordsx))
          coordsy-f           (if coordsx (Double/parseDouble coordsy))]
          (try
              (if (use-binary-rendering? use-binary? drawing-name)
                  nil
                 ;(find-room-from-binary-data image drawing-id drawing-name
                 ;                 width height
                 ;                 user-x-offset user-y-offset user-scale
                 ;                 selected room-colors coordsx-f coordsy-f
                 ;                 debug)
                  (find-room drawing-id drawing-name
                             width height
                             (- user-x-offset default-x-offset) user-y-offset user-scale
                             coordsx-f coordsy-f use-memory-cache h-center v-center))
              (catch Exception e
                  (log/error "error during finding room!" e)))))


(defn raster-drawing
    "REST API handler for the /api/raster-drawing endpoint."
    [request]
    (let [start-time          (System/currentTimeMillis)
          input-stream        (perform-raster-drawing request)
          end-time            (System/currentTimeMillis)]
          (log/info "Rendering time (ms):" (- end-time start-time))
          (log/info "Image size (bytes): " (.available input-stream))
          (http-utils/png-response input-stream)))


(defn find-room-on-drawing
    "REST API handler to find room on drawing."
    [request]
    (let [start-time          (System/currentTimeMillis)
          room                (perform-find-room request)
          end-time            (System/currentTimeMillis)]
          (log/info "Finding time (ms):" (- end-time start-time))
          (-> (http-response/response room)
              (http-response/content-type "text/plain")
              (http-response/status 200))))
