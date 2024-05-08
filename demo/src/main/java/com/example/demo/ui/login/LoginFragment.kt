package com.example.demo.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.demo.R
import com.example.demo.databinding.FragmentLoginBinding
import com.mobile.vms.VMSMobileSDK

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        val loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        loginViewModel.loginState.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it.isLoading) View.VISIBLE else View.GONE
            binding.buttonLogin.visibility = if (it.isLoading) View.INVISIBLE else View.VISIBLE
            binding.tvError.text = it.errorText ?: ""
            binding.tvError.visibility =
                if (it.errorText.isNullOrEmpty() || it.isLoading) View.INVISIBLE else View.VISIBLE
            it.successToken?.let { token -> successLogin(token) }
        }

        binding.buttonLogin.setOnClickListener {
            loginViewModel.loginPressed(
                binding.etLogin.text.toString(),
                binding.etPassword.text.toString()
            )
        }

        return binding.root
    }

    private fun successLogin(token: String) {
        VMSMobileSDK.userToken = token   // save token to sdk
        findNavController().navigate(R.id.action_navigation_login_to_navigation_cameras)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}