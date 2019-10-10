package id.oddbit.flutter.facebook_app_events

import android.os.Bundle
import android.util.Log
import com.facebook.appevents.AppEventsLogger
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class FacebookAppEventsPlugin(registrar: Registrar) : MethodCallHandler {
  private val logTag = "FacebookAppEvents"
  var logger: AppEventsLogger

  init {
    this.logger = AppEventsLogger.newLogger(registrar.context())
  }

  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "flutter.oddbit.id/facebook_app_events")
      channel.setMethodCallHandler(FacebookAppEventsPlugin(registrar))
    }
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "clearUserData" -> handleClearUserData(call, result)
      "clearUserID" -> handleClearUserId(call, result)
      "getPlatformVersion" -> handlePlatformVersion(call, result)
      "logEvent" -> handleLogEvent(call, result)
      "setUserData" -> handleSetUserData(call, result)
      "setUserID" -> handleSetUserId(call, result)
      "updateUserProperties" -> handleUpdateUserProperties(call, result)
      else -> result.notImplemented()
    }
  }

  private fun handleLogEvent(call: MethodCall, result: Result) {
    val eventName = call.argument("name") as? String

    val parameters = call.argument("parameters") as? Map<String, Object>
    val parameterBundle = createBundleFromMap(parameters)

    val valueToSum = call.argument("valueToSum") as? Double

    logEvent(eventName, parameterBundle, valueToSum)

    result.success(null)
  }

  private fun handleSetUserData(call: MethodCall, result: Result) {
    val parameters = call.argument("parameters") as? Map<String, Object>
    val parameterBundle = createBundleFromMap(parameters)

    AppEventsLogger.setUserData(
      parameterBundle?.getString("email"),
      parameterBundle?.getString("firstName"),
      parameterBundle?.getString("lastName"),
      parameterBundle?.getString("phone"),
      parameterBundle?.getString("dateOfBirth"),
      parameterBundle?.getString("gender"),
      parameterBundle?.getString("city"),
      parameterBundle?.getString("state"),
      parameterBundle?.getString("zip"),
      parameterBundle?.getString("country")
    )

    result.success(null)
  }

  private fun handleUpdateUserProperties(call: MethodCall, result: Result) {
    val applicationId = call.argument("applicationId") as? String
    val parameters = call.argument("parameters") as? Map<String, Object>
    val parameterBundle = createBundleFromMap(parameters) ?: Bundle()

    val requestCallback = GraphRequest.Callback() {
      @Override
      fun onCompleted(response: GraphResponse) {
        val data = response.getJSONObject()
        result.success(data)
      }
    }

    for (key in parameterBundle.keySet()) {
      Log.d(logTag, "[updateUserProperties] " + key + ": " + parameterBundle.get(key))
    }

    if (applicationId == null) AppEventsLogger.updateUserProperties(parameterBundle, requestCallback)
    else AppEventsLogger.updateUserProperties(parameterBundle, applicationId, requestCallback)
  }

  private fun handleClearUserData(call: MethodCall, result: Result) {
    AppEventsLogger.clearUserData()
    result.success(null)
  }

  private fun handleSetUserId(call: MethodCall, result: Result) {
    val id = call.arguments as String
    AppEventsLogger.setUserID(id)
    result.success(null)
  }

  private fun handleClearUserId(call: MethodCall, result: Result) {
    AppEventsLogger.clearUserID()
    result.success(null)
  }

  private fun handlePlatformVersion(call: MethodCall, result: Result) {
    result.success("Android ${android.os.Build.VERSION.RELEASE}")
  }


  fun logEvent(eventName: String?, params: Bundle?, valToSum: Double?) {
    if (valToSum != null) logger.logEvent(eventName, valToSum, params)
    else logger.logEvent(eventName, params)
  }

  private fun createBundleFromMap(parameterMap: Map<String, Any>?): Bundle? {
    if (parameterMap == null) {
      return null
    }

    val bundle = Bundle()
    for (jsonParam in parameterMap.entries) {
      val value = jsonParam.value
      val key = jsonParam.key
      if (value is String) {
        bundle.putString(key, value as String)
      } else if (value is Int) {
        bundle.putInt(key, value as Int)
      } else if (value is Long) {
        bundle.putLong(key, value as Long)
      } else if (value is Double) {
        bundle.putDouble(key, value as Double)
      } else if (value is Boolean) {
        bundle.putBoolean(key, value as Boolean)
      } else if (value is Map<*, *>) {
        val nestedBundle = createBundleFromMap(value as Map<String, Any>)
        bundle.putBundle(key, nestedBundle as Bundle)
      } else {
        throw IllegalArgumentException(
            "Unsupported value type: " + value.javaClass.kotlin)
      }
    }
    return bundle
  }
}