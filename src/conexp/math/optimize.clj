;; Copyright (c) Daniel Borchmann. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns conexp.math.optimize
  (:use conexp.math.util
	conexp.base)
  (:import [org.apache.commons.math.optimization RealPointValuePair GoalType
	                                         DifferentiableMultivariateRealOptimizer
	                                         RealConvergenceChecker]
	   [org.apache.commons.math.optimization.direct DirectSearchOptimizer NelderMead MultiDirectional]
	   [org.apache.commons.math.optimization.general AbstractScalarDifferentiableOptimizer
	                                                 NonLinearConjugateGradientOptimizer
	                                                 ConjugateGradientFormula]))

;;;

(defn- customize-optimizer
  "Customizes given optimizer with options."
  [optimizer options]
  (when (:iterations options)
    (let [#^RealConvergenceChecker cc (.getConvergenceChecker optimizer)]
      (.setConvergenceChecker optimizer
			      (proxy [RealConvergenceChecker] []
				(converged [iterations previous current]
				  (or (.converged cc iterations previous current)
				      (>= iterations (:iterations options))))))))
  optimizer)

(defn- make-direct-optimizer
  "Direct optimizer used by directly-optimize."
  [options]
  (doto (NelderMead.)
    (customize-optimizer options)))

(defn- make-differential-optimizer
  "Optimizer for differentiable functions used by differentially-optimize."
  [options]
  (doto (NonLinearConjugateGradientOptimizer. ConjugateGradientFormula/FLETCHER_REEVES)
    (customize-optimizer options)))

(defn- point-value-pair-to-vector
  "Converts RealPointValuePair to a point-value-vector."
  [#^RealPointValuePair pvp]
  [(vec (.getPoint pvp)) (.getValue pvp)])

(defn- directly-optimize
  "Optimizes fn according to goal as given by *direct-optimizer*."
  [fn starting-point goal options]
  (let [point-value-pair (.optimize (make-direct-optimizer options)
				    (as-multivariate-real-fn fn)
				    goal
				    (into-array Double/TYPE starting-point))]
    (point-value-pair-to-vector point-value-pair)))

(defn- differentially-optimize
  "Optimizes fn according to goal as given by
  *differential-optimizer*. partial-derivatives must be a function
  computing the k-th partial derivation (as clojure function) when
  given k."
  [fn partial-derivatives starting-point goal options]
  (let [point-value-pair (.optimize (make-differential-optimizer options)
				    (as-differentiable-multivariate-real-fn
				     fn
				     (count starting-point)
				     partial-derivatives)
				    goal
				    (into-array Double/TYPE starting-point))]
    (point-value-pair-to-vector point-value-pair)))

(defn minimize
  "Minimizes fn starting at starting-point. When given
  partial-derivatives uses a differential optimizer, otherwise uses a
  direct one."
  ([fn starting-point options]
     (directly-optimize fn starting-point GoalType/MINIMIZE options))
  ([fn partial-derivatives starting-point options]
     (differentially-optimize fn partial-derivatives starting-point GoalType/MINIMIZE options)))

(comment
  Use this way

  (minimize #(Math/sin %)
	    (fn [k]
	      (condp = k
		0 #(Math/cos %)
		(constantly 0.0)))
	    [0.0])

  -> [[-1.5707963267948966] -1.0]
)

(defn maximize
  "Maximizes fn starting at starting-point. When given
  partial-derivatives uses a differential optimizer, otherwise uses a
  direct one."
  ([fn starting-point options]
     (directly-optimize fn starting-point GoalType/MAXIMIZE options))
  ([fn partial-derivatives starting-point options]
     (differentially-optimize fn partial-derivatives starting-point GoalType/MAXIMIZE options)))

;;;

nil
