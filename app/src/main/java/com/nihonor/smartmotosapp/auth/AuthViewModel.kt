package com.nihonor.smartmotosapp.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

sealed class OtpState {
    object Idle : OtpState()
    object SendingCode : OtpState()
    data class CodeSent(val verificationId: String) : OtpState()
    object VerifyingCode : OtpState()
    object Verified : OtpState()
    data class Error(val message: String) : OtpState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var storedVerificationId: String? = null

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _otpState.value = OtpState.SendingCode

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Android-only auto-retrieval path — sign in immediately without user typing the code.
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _otpState.value = OtpState.Error(e.message ?: "Verification failed")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificationId
                resendToken = token
                _otpState.value = OtpState.CodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        val verificationId = storedVerificationId
        if (verificationId == null) {
            _otpState.value = OtpState.Error("No verification in progress")
            return
        }
        _otpState.value = OtpState.VerifyingCode
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credential).await()
                _otpState.value = OtpState.Verified
            } catch (e: Exception) {
                _otpState.value = OtpState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    fun reset() {
        _otpState.value = OtpState.Idle
        storedVerificationId = null
        resendToken = null
    }
}
