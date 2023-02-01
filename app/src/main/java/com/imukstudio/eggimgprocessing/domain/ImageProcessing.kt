package com.imukstudio.eggimgprocessing.domain

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import com.imukstudio.eggimgprocessing.App
import com.imukstudio.eggimgprocessing.domain.model.EggImageObject
import com.imukstudio.eggimgprocessing.domain.model.EggParameters
import com.imukstudio.eggimgprocessing.domain.model.ReferenceObject
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class ImageProcessing {
    private var eggImageObject: EggImageObject? = null
    private var referenceObject: ReferenceObject? = null
    private var eggParamsListener: ((params: EggParameters) -> Unit)? = null

    fun processImage(img: Bitmap): Bitmap =
        processImageInner(img)

    fun setEggParamListener(listener: (params: EggParameters) -> Unit) {
        eggParamsListener = listener
    }

    private fun processImageInner(value: Bitmap): Bitmap {
        val value1 = value.scale(
            value.width / RESIZE_IMG_COEFFICIENT,
            value.height / RESIZE_IMG_COEFFICIENT
        )
        val mat = Mat()
        val rgbImgMat = Mat()

        Utils.bitmapToMat(value1, mat)
        mat.copyTo(rgbImgMat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(mat, mat, Size(7.0, 7.0), 0.0)
        Imgproc.Canny(mat, mat, 50.0, 100.0)
        Imgproc.dilate(mat, mat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0)))
        Imgproc.erode(mat, mat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0)))

        val cnts = mutableListOf<MatOfPoint>()
        Imgproc.findContours(mat, cnts, Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
        Imgproc.drawContours(rgbImgMat, cnts, -1, Scalar(255.0, 0.0, 0.0), 2)

//        cnts.forEach {
//            val boundingRect = Imgproc.boundingRect(it)
//
//            if (boundingRect.height * boundingRect.width > 500
//                && boundingRect.height != value1.height) {
//                if (isReferenceObject(boundingRect)) {
//                    // We find reference object
//                    Imgproc.rectangle(rgbImgMat, boundingRect, Scalar(0.0, 255.0, 0.0), 1)
//                    Imgproc.putText(
//                        rgbImgMat,
//                        "Reference h=${boundingRect.height} w=${boundingRect.width}",
//                        Point(boundingRect.x.toDouble(), boundingRect.y.toDouble() - 5),
//                        1,
//                        0.7,
//                        Scalar(0.0, 255.0, 0.0)
//                    )
//                    val sizeInImg = (boundingRect.width + boundingRect.height) / 2
//                    referenceObject = ReferenceObject(
//                        boundingBox = boundingRect,
//                        coefficient = REFERENCE_OBJECT_REAL_DIAMETER_MM / sizeInImg,
//                        type = ReferenceObjectType.CIRCLE
//                    )
//                } else {
//                    Imgproc.rectangle(rgbImgMat, boundingRect, Scalar(0.0, 255.0, 0.0), 1)
//                    Imgproc.putText(
//                        rgbImgMat,
//                        "Egg h=${boundingRect.height} w=${boundingRect.width}",
//                        Point(boundingRect.x.toDouble(), boundingRect.y.toDouble() - 5),
//                        1,
//                        0.7,
//                        Scalar(0.0, 255.0, 0.0)
//                    )
//
//                    eggImageObject = getEggObject(boundingRect, it)
//                }
//                drawBoundingRectCornerPoints(rgbImgMat, boundingRect, it)
//            }
//        }
//        if (referenceObject == null) {
//            throw IllegalStateException("Can't find reference object!")
//        }
//        if (eggImageObject == null) {
//            throw IllegalStateException("Can't find egg object!")
//        }
//        calculateEggParams(eggImageObject!!)
        Utils.matToBitmap(rgbImgMat, value1)
        return value1
    }

    private fun getEggObject(rect: Rect, cnt: MatOfPoint): EggImageObject {
        // find height axis line points
        val A = Point((rect.x + (rect.x + rect.width)).toDouble() / 2, rect.y.toDouble())
        val B = Point((rect.x + (rect.x + rect.width)).toDouble() / 2, rect.y.toDouble() + rect.height)

        // Find small axis line points for getting cross the axises
        val leftPointSmallAxis = findPointOfSmallAxis(cnt, rect.x.toDouble())
        val rightPointSmallAxis = findPointOfSmallAxis(cnt, (rect.x + rect.width).toDouble())

        val intersectionPoint = lineLineIntersection(A, B, leftPointSmallAxis, rightPointSmallAxis)

        val highAxisPointALineWidth = abs(intersectionPoint.y - A.y)
        val highAxisPointBLineWidth = abs(B.y - intersectionPoint.y)

        return EggImageObject(
            rect,
            rect.width.toDouble() / 2,
            highAxisPointBLineWidth,
            highAxisPointALineWidth,
            intersectionPoint
        )
    }

    private fun drawBoundingRectCornerPoints(rgbImgMat: Mat, rect: Rect, cnt: MatOfPoint) {
        Imgproc.circle(rgbImgMat, Point(rect.x.toDouble(), rect.y.toDouble()), 5, Scalar(0.0, 255.0, 0.0), 1)
        Imgproc.circle(rgbImgMat, Point(rect.x.toDouble() + rect.width, rect.y.toDouble()), 5, Scalar(0.0, 255.0, 0.0), 1)
        Imgproc.circle(rgbImgMat, Point(rect.x.toDouble() + rect.width, rect.y.toDouble() + rect.height), 5, Scalar(0.0, 255.0, 0.0), 1)
        Imgproc.circle(rgbImgMat, Point(rect.x.toDouble(), rect.y.toDouble() + rect.height), 5, Scalar(0.0, 255.0, 0.0), 1)

        val A = Point((rect.x + (rect.x + rect.width)).toDouble() / 2, rect.y.toDouble())
        val B = Point((rect.x + (rect.x + rect.width)).toDouble() / 2, rect.y.toDouble() + rect.height)
        Imgproc.line(rgbImgMat, A, B, Scalar(0.0, 255.0, 0.0), 1)

        val leftPointSmallAxis = findPointOfSmallAxis(cnt, rect.x.toDouble())
        val rightPointSmallAxis = findPointOfSmallAxis(cnt, (rect.x + rect.width).toDouble())
        Imgproc.line(rgbImgMat, leftPointSmallAxis, rightPointSmallAxis, Scalar(0.0, 255.0, 0.0), 1)

        val intersectionPoint = lineLineIntersection(A, B, leftPointSmallAxis, rightPointSmallAxis)
        Imgproc.circle(rgbImgMat, intersectionPoint, 5, Scalar(0.0, 255.0, 0.0), 1)
    }

    private fun lineLineIntersection(A: Point, B: Point, C: Point, D: Point): Point {
        // Line AB represented as a1x + b1y = c1
        val a1 = B.y - A.y
        val b1 = A.x - B.x
        val c1 = a1 * A.x + b1 * A.y

        // Line CD represented as a2x + b2y = c2
        val a2 = D.y - C.y
        val b2 = C.x - D.x
        val c2 = a2 * C.x + b2 * C.y
        val determinant = a1 * b2 - a2 * b1
        return if (determinant == 0.0) {
            // The lines are parallel. This is simplified
            // by returning a pair of FLT_MAX
            Point(Double.MAX_VALUE, Double.MAX_VALUE)
        } else {
            val x = (b2 * c1 - b1 * c2) / determinant
            val y = (a1 * c2 - a2 * c1) / determinant
            Point(x, y)
        }
    }

    private fun findPointOfSmallAxis(cnt: MatOfPoint, x: Double): Point {
        var averageYLLeftSideSum = 0.0
        var averageYLLeftSideCount = 0
        cnt.toArray().forEach { point ->
            if (point.x in x-5..x+5) {
                averageYLLeftSideCount++
                averageYLLeftSideSum += point.y
            }
        }
        return Point(x, averageYLLeftSideSum / averageYLLeftSideCount)
    }

    private fun isReferenceObject(boundingRect: Rect): Boolean =
        abs(boundingRect.width - boundingRect.height) in 0..5

    private fun calculateEggParams(eggImageObject: EggImageObject) {
        val eggRealWidth = eggImageObject.boundingBox.width * referenceObject!!.coefficient
        val eggRealHeight = eggImageObject.boundingBox.height * referenceObject!!.coefficient
        val a = eggImageObject.a * referenceObject!!.coefficient
        val b = eggImageObject.b * referenceObject!!.coefficient
        val c = eggImageObject.c * referenceObject!!.coefficient

        Log.d(App.APP_LOG_TAG, "Physical length of egg radius: a = $a b = $b c = $c")
        Log.d(App.APP_LOG_TAG, "Egg real size: w = $eggRealWidth h = $eggRealHeight")

        val eggVolume = (2 * Math.PI / 3) * a.pow(2) * (b + c)

        val first = b.pow(2) / sqrt(abs(b.pow(2) - a.pow(2))) * acos(a / b)
        val second = c.pow(2) / sqrt(abs(c.pow(2) - a.pow(2))) * acos(a / c)
        val eggSquare = (2 * Math.PI * a.pow(2)) + Math.PI * a * (first + second)

        val eggMass = eggVolume / 1000 * EGG_DENSITY

        val eggAreaToVolume = eggSquare / eggVolume
        val eggShellMass = eggMass * EGG_SHELL_MASS_PERCENT_COEFFICIENT
        val eggYolkMass = eggMass * EGG_YOLK_MASS_PERCENT_COEFFICIENT
        val eggProteinMass = eggMass * EGG_PROTEIN_MASS_PERCENT_COEFFICIENT

        eggParamsListener?.invoke(
            EggParameters(
                width = eggRealWidth,
                height = eggRealHeight,
                volume = eggVolume,
                square = eggSquare,
                mass = eggMass,
                rationAreaToVolume = eggAreaToVolume,
                shellMass = eggShellMass,
                yolkMass = eggYolkMass,
                proteinMass = eggProteinMass
            )
        ) ?: throw IllegalStateException("ImageProcessing() object: eggParamListener didn't set!")
    }

//    private fun findEllipseObject(cnt: MatOfPoint, boundingRect: Rect): Boolean =
//        if (cnt.toArray().size > 5) {
//            val ellipse = Imgproc.fitEllipse(MatOfPoint2f(*cnt.toArray()))
//
//            (ellipse.size.width.toInt() - boundingRect.width) < 8 &&
//                    abs(ellipse.size.height.toInt() - boundingRect.height) < 8
//        } else {
//            false
//        }

    companion object {
        private const val REFERENCE_OBJECT_REAL_DIAMETER_MM = 20.5
        private const val EGG_DENSITY = 1.09
        private const val EGG_SHELL_MASS_PERCENT_COEFFICIENT = 0.115
        private const val EGG_YOLK_MASS_PERCENT_COEFFICIENT = 0.31
        private const val EGG_PROTEIN_MASS_PERCENT_COEFFICIENT = 0.56
        private const val RESIZE_IMG_COEFFICIENT = 10
    }
}
