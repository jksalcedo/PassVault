package com.jksalcedo.passvault.ui.settings

import android.content.pm.PackageInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.jksalcedo.passvault.R
import com.jksalcedo.passvault.databinding.FragmentAboutBinding

/**
 * A fragment for displaying information about the app.
 */
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = FragmentAboutBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.tvVersion.text = try {
            val packageName = requireContext().packageName
            val packageInfo: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requireContext().packageManager.getPackageInfo(packageName, 0)
                } else {
                    @Suppress("DEPRECATION")
                    requireContext().packageManager.getPackageInfo(packageName, 0)
                }
            val versionName = packageInfo.versionName ?: "Unknown"
            getString(R.string.app_version_label, versionName)
        } catch (_: Exception) {
            getString(R.string.app_version_label, "Unknown")
        }

        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.about)

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}