package com.example.notes.ui.login

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.notes.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class LoginFragment : Fragment() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val auth = FirebaseAuth.getInstance()
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                activity?.let {
                    Toast.makeText(it, "Ошибка Google Sign-In: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Кнопка Google Sign-In
        view.findViewById<View>(R.id.google_sign_in_button).setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        // Кнопка Email/Password
        view.findViewById<View>(R.id.email_sign_in_button).setOnClickListener {
            showEmailPasswordDialog()
        }

        // Проверка авторизации
        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
        }

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
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                activity?.let { // Безопасный доступ к активности
                    if (task.isSuccessful) {
                        Toast.makeText(it, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        Toast.makeText(it, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                activity?.let { // Безопасный доступ к активности
                    if (task.isSuccessful) {
                        Toast.makeText(it, "Вход успешен", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        Toast.makeText(it, "Ошибка: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                activity?.let { // Безопасный доступ к активности
                    if (task.isSuccessful) {
                        Toast.makeText(it, "Вход успешен", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                    } else {
                        Toast.makeText(it, "Ошибка аутентификации", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}