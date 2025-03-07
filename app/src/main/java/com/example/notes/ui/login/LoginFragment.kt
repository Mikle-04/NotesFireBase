package com.example.notes.ui.login

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notes.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class LoginFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth = FirebaseAuth.getInstance()
    private lateinit var loginProgress: ProgressBar

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Ошибка Google Sign-In: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginProgress = view.findViewById(R.id.login_progress)

        // Настройка Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Проверка авторизации
        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            return
        }

        // Кнопка Google Sign-In
        view.findViewById<MaterialButton>(R.id.google_sign_in_button).setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        // Кнопка Email/Password
        view.findViewById<MaterialButton>(R.id.email_sign_in_button).setOnClickListener {
            showEmailPasswordDialog()
        }

        // Анимации появления
        view.findViewById<View>(R.id.logo_image).animate().alpha(1f).setDuration(500).start()
        view.findViewById<View>(R.id.title).animate().alpha(1f).setDuration(500).setStartDelay(200).start()
        view.findViewById<View>(R.id.google_sign_in_button).animate().alpha(1f).setDuration(500).setStartDelay(400).start()
        view.findViewById<View>(R.id.email_sign_in_button).animate().alpha(1f).setDuration(500).setStartDelay(600).start()
    }

    private fun showEmailPasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_email_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.password_edit_text)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Вход через Email")
            .setView(dialogView)
            .setPositiveButton("Войти") { _, _ ->
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    signInWithEmail(email, password)
                } else {
                    Toast.makeText(requireContext(), "Введите email и пароль", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Зарегистрироваться") { _, _ ->
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    registerWithEmail(email, password)
                } else {
                    Toast.makeText(requireContext(), "Введите email и пароль", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun registerWithEmail(email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loginProgress.visibility = View.VISIBLE
                auth.createUserWithEmailAndPassword(email, password).await()
                loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Регистрация успешна", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            } catch (e: Exception) {
                loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Ошибка регистрации: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loginProgress.visibility = View.VISIBLE
                auth.signInWithEmailAndPassword(email, password).await()
                loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Вход успешен", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            } catch (e: Exception) {
                loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loginProgress.visibility = View.VISIBLE
                auth.signInWithCredential(credential).await()
                loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Вход успешен", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
            } catch (e: Exception) {
                loginProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Ошибка аутентификации: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}