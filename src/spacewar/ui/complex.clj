(ns spacewar.ui.complex
  (:require [quil.core :as q]
            [spacewar.ui.protocols :as p]
            [spacewar.ui.view-frame :as f]
            [spacewar.ui.control-panels :as cp]
            [spacewar.ui.widgets :as w]))

(defn draw-light-panel [state]
  (let [{:keys [x y w h indicators background]} state]
    (apply q/fill background)
    (q/no-stroke)
    (q/rect x y w h)
    (doseq [indicator indicators] (p/draw indicator))))

(defn update-light-panel [state]
  (let [{:keys [indicators level-func]} state
        indicator-states (map p/get-state indicators)
        new-indicators (map-indexed #(w/->indicator-light (assoc %2 :level (level-func %1))) indicator-states)
        new-state (assoc state :indicators new-indicators)]
    new-state))

(defn shift-pattern [n i]
  (if (= i (rem (quot (q/frame-count) 3) n)) 1 0))

(defn random-pattern [_ _]
  (rand-int 2))

(defn build-indicators [state]
  (let [{:keys [x y w h rows columns gap indicator-height indicator-width draw-func colors]} state
        cell-width (/ (- w gap gap) columns)
        cell-height (/ (- h gap gap) rows)
        cell-x-offset (- (/ cell-width 2) (/ indicator-width 2))
        cell-y-offset (- (/ cell-height 2) (/ indicator-height 2))
        indicators (for [row (range rows) column (range columns)]
                     (p/setup
                       (w/->indicator-light
                         {:x (+ x gap cell-x-offset (* cell-width column))
                          :y (+ y gap cell-y-offset (* cell-height row))
                          :w indicator-width
                          :h indicator-height
                          :level 0
                          :draw-func draw-func
                          :colors colors})))]
    indicators))

(deftype bottom-lights [state]
  p/Drawable
  (draw [_] (draw-light-panel state))

  (setup [_]
    (let [rows 1
          columns 14
          indicators (build-indicators
                       (assoc state :rows rows
                                    :columns columns
                                    :gap 20
                                    :indicator-height 10
                                    :indicator-width 20
                                    :draw-func q/rect
                                    :colors [[50 50 50] [255 255 255]]))
          new-state (assoc state :indicators indicators
                                 :level-func (partial shift-pattern (* rows columns))
                                 :background [150 150 150])]
      (bottom-lights. new-state)))

  (update-state [_ _] (p/pack-update (bottom-lights. (update-light-panel state)))))

(deftype side-lights [state]
  p/Drawable
  (draw [_] (draw-light-panel state))

  (setup [_]
    (let [rows 10
          columns 2
          indicators (build-indicators
                       (assoc state :rows rows
                                    :columns columns
                                    :gap 20
                                    :indicator-height 15
                                    :indicator-width 15
                                    :draw-func q/ellipse
                                    :colors [[50 50 50] [255 255 0]]))
          new-state (assoc state :indicators indicators
                                 :level-func (partial random-pattern (* rows columns))
                                 :background [150 50 50])]
      (side-lights. new-state)))

  (update-state [this _]
    (p/pack-update
      (if (zero? (rand-int 15))
        (side-lights. (update-light-panel state))
        this))))

(deftype complex [state]
  p/Drawable
  (draw [_] (p/draw-elements state))

  (setup [_]
    (let [{:keys [x y h w]} state
          left-margin 200
          right-margin 200
          bottom-margin 200
          panel-gap 20
          small-panel-gap 10

          frame-width (- w left-margin right-margin)
          frame-height (- h bottom-margin)
          frame-bottom (+ y frame-height)
          frame (p/setup
                  (f/->frame {:x (+ x left-margin)
                              :y y
                              :h frame-height
                              :w frame-width}))

          bottom-lights-width (/ frame-width 2)
          bottom-lights-left-offset (/ (- frame-width bottom-lights-width) 2)
          bottom-lights-x (+ x left-margin bottom-lights-left-offset)
          bottom-lights-y (+ y (- h bottom-margin) panel-gap)
          bottom-lights-h 40
          bottom-lights (p/setup
                          (->bottom-lights {:x bottom-lights-x
                                            :y bottom-lights-y
                                            :h bottom-lights-h
                                            :w bottom-lights-width}))

          side-panel-height (/ frame-height 2.5)
          side-panel-width 120
          side-panel-y (+ y (/ frame-height 5))
          left-lights (p/setup
                        (->side-lights {:x (- (+ x left-margin) panel-gap side-panel-width)
                                        :y side-panel-y
                                        :h side-panel-height
                                        :w side-panel-width}))

          right-lights (p/setup
                         (->side-lights {:x (+ x left-margin frame-width panel-gap)
                                         :y side-panel-y
                                         :w side-panel-width
                                         :h side-panel-height}))

          scan-panel-x (+ small-panel-gap x)
          scan-panel-y (+ side-panel-y side-panel-height panel-gap)
          scan-panel-w (- left-margin (* 2 small-panel-gap))
          scan-panel-h (- frame-bottom side-panel-height side-panel-y panel-gap)
          scan-panel (p/setup
                       (cp/->scan-panel {:x scan-panel-x
                                         :y scan-panel-y
                                         :w scan-panel-w
                                         :h scan-panel-h
                                         :name "SCAN"
                                         :color [150 150 255]
                                         :button-color [100 100 255]}))

          engine-panel-x scan-panel-x
          engine-panel-y (+ y frame-height small-panel-gap)
          engine-panel-w (+ left-margin bottom-lights-left-offset (- (* 2 small-panel-gap)))
          engine-panel-h (- bottom-margin small-panel-gap)
          engine-panel (p/setup
                         (cp/->engine-panel {:x engine-panel-x
                                             :y engine-panel-y
                                             :w engine-panel-w
                                             :h engine-panel-h
                                             :name "ENGINES"
                                             :color [150 255 150]
                                             :button-color [80 255 80]}))

          weapons-panel-x (+ bottom-lights-x bottom-lights-width small-panel-gap)
          weapons-panel-y (+ y frame-height small-panel-gap)
          weapons-panel-w (- w left-margin bottom-lights-left-offset bottom-lights-width panel-gap)
          weapons-panel-h (- bottom-margin small-panel-gap)
          weapons-panel (p/setup
                          (cp/->weapons-panel {:x weapons-panel-x
                                               :y weapons-panel-y
                                               :w weapons-panel-w
                                               :h weapons-panel-h
                                               :name "WEAPONS"
                                               :color [255 200 50]
                                               :button-color [255 150 50]
                                               :inverted true}))

          damage-panel-x (+ x left-margin frame-width small-panel-gap)
          damage-panel-y scan-panel-y
          damage-panel-w (- right-margin (* 2 small-panel-gap))
          damage-panel-h scan-panel-h
          damage-panel (p/setup
                         (cp/->damage-panel {:x damage-panel-x
                                             :y damage-panel-y
                                             :w damage-panel-w
                                             :h damage-panel-h
                                             :name "DAMAGE"
                                             :color [255 100 100]
                                             :inverted true}))

          status-panel-x (+ engine-panel-x engine-panel-w small-panel-gap )
          status-panel-y (+ bottom-lights-y bottom-lights-h panel-gap)
          status-panel-w bottom-lights-width
          status-panel-h (- h frame-height panel-gap bottom-lights-h)
          status-panel (p/setup
                         (cp/->status-panel {:x status-panel-x
                                             :y status-panel-y
                                             :w status-panel-w
                                             :h status-panel-h
                                             :name "STATUS"
                                             :color [255 255 150]}))

          new-state (assoc state :frame frame
                                 :bottom-lights bottom-lights
                                 :left-lights left-lights
                                 :right-lights right-lights
                                 :scan-panel scan-panel
                                 :engine-panel engine-panel
                                 :weapons-panel weapons-panel
                                 :damage-panel damage-panel
                                 :status-panel status-panel
                                 :elements [:frame :bottom-lights :left-lights
                                            :right-lights :scan-panel
                                            :engine-panel :weapons-panel
                                            :damage-panel :status-panel])]
      (complex. new-state)))

  (update-state [_ commands]
    (let [[new-state events] (p/update-elements state commands)]
      (p/pack-update
        (complex. new-state)
        events))))




