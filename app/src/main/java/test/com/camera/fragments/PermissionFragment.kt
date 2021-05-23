package test.com.camera.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import test.com.camera.R

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PermissionFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(hasPermissions(requireContext())){
            // If permissions have already been granted, proceed
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(R.id.action_permission_to_camera)
        } else {
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSIONS_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Navigation.findNavController(requireActivity(), R.id.fragment_container)
                    .navigate(R.id.action_permission_to_camera)
            }
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private val TAG = PermissionFragment::class.java.simpleName
        fun hasPermissions(context:Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}