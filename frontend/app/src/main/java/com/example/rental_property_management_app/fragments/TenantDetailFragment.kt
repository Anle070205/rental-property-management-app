package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantDetailBinding
import com.example.rental_property_management_app.models.ProvisionTenantAccountRequest
import com.example.rental_property_management_app.models.TenantResponse
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.TenantViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TenantDetailFragment : Fragment() {

    private var _binding: FragmentTenantDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TenantViewModel by viewModels()
    private val args: TenantDetailFragmentArgs by navArgs()
    private var currentTenant: TenantResponse? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeTenantDetail()
        observeDeleteState()
        observeProvisionState()

        viewModel.loadTenantDetail(args.tenantId)
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEdit.setOnClickListener {
            val action = TenantDetailFragmentDirections.actionTenantDetailToTenantForm(tenantId = args.tenantId)
            findNavController().navigate(action)
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm)
                .setMessage(R.string.confirm_delete_tenant)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.deleteTenant(args.tenantId)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.btnProvisionAccount.setOnClickListener {
            val tenant = currentTenant ?: return@setOnClickListener
            showProvisionDialog(tenant)
        }

        binding.btnResendActivation.setOnClickListener {
            val tenant = currentTenant ?: return@setOnClickListener
            val defaultChannel = if (!tenant.email.isNullOrBlank()) "EMAIL" else "PHONE"
            viewModel.resendTenantActivation(tenant.id, defaultChannel)
        }

        binding.btnViewContracts.setOnClickListener {
            val tenant = currentTenant ?: return@setOnClickListener
            findNavController().navigate(
                R.id.contractListFragment,
                bundleOf("tenantId" to tenant.id)
            )
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showProvisionDialog(tenant: TenantResponse) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 12)
        }
        val rgChannel = RadioGroup(ctx).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        val rbEmail = RadioButton(ctx).apply {
            id = View.generateViewId()
            text = "Email / Gmail"
        }
        val rbPhone = RadioButton(ctx).apply {
            id = View.generateViewId()
            text = "Số điện thoại"
        }
        rgChannel.addView(rbEmail)
        rgChannel.addView(rbPhone)

        val etTarget = EditText(ctx).apply {
            hint = "Email hoặc SĐT kích hoạt"
        }
        val cbLinkExisting = CheckBox(ctx).apply {
            text = "Xác nhận liên kết nếu email/SĐT đã có tài khoản"
        }

        if (!tenant.email.isNullOrBlank()) {
            rbEmail.isChecked = true
            etTarget.setText(tenant.email)
        } else {
            rbPhone.isChecked = true
            etTarget.setText(tenant.phone.orEmpty())
        }

        rgChannel.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == rbEmail.id) {
                etTarget.setText(tenant.email.orEmpty())
            } else {
                etTarget.setText(tenant.phone.orEmpty())
            }
        }

        container.addView(rgChannel)
        container.addView(etTarget)
        container.addView(cbLinkExisting)

        AlertDialog.Builder(ctx)
            .setTitle("Cấp tài khoản khách thuê")
            .setMessage("Không gửi mật khẩu plaintext. Hệ thống sẽ tạo tài khoản ở trạng thái chờ kích hoạt và gửi OTP để khách tự đặt mật khẩu.")
            .setView(container)
            .setPositiveButton("Gửi OTP kích hoạt") { _, _ ->
                val channel = if (rbEmail.isChecked) "EMAIL" else "PHONE"
                val target = etTarget.text.toString().trim()
                if (target.isEmpty()) {
                    Toast.makeText(ctx, "Vui lòng nhập Email hoặc SĐT", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.provisionTenantAccount(
                        tenant.id,
                        ProvisionTenantAccountRequest(
                            channel = channel,
                            target = target,
                            confirmLinkExisting = cbLinkExisting.isChecked
                        )
                    )
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun observeProvisionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.provisionState.collectLatest { state ->
                state ?: return@collectLatest
                when (state) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val res = state.data
                        AlertDialog.Builder(requireContext())
                            .setTitle("Trạng thái tài khoản khách thuê")
                            .setMessage(res?.message ?: "Đã gửi OTP kích hoạt tài khoản cho khách thuê.")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetProvisionState()
                    }
                    is Resource.Error -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Không thể hoàn tất cấp tài khoản")
                            .setMessage(state.message ?: "Vui lòng kiểm tra lại cấu hình gửi OTP hoặc thông tin liên hệ.")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetProvisionState()
                    }
                }
            }
        }
    }

    private fun observeDeleteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteState.collectLatest { state ->
                when {
                    state.isLoading -> Unit
                    state.data != null -> {
                        Toast.makeText(context, getString(R.string.tenant_deleted), Toast.LENGTH_SHORT).show()
                        viewModel.resetDeleteState()
                        findNavController().navigateUp()
                    }
                    state.error != null -> {
                        Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
                        viewModel.resetDeleteState()
                    }
                }
            }
        }
    }

    private fun observeTenantDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tenantDetailState.collectLatest { state ->
                when {
                    state.isLoading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.contentLayout.visibility = View.GONE
                    }
                    state.data != null -> {
                        binding.progressBar.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        displayTenantDetail(state.data)
                    }
                    state.error != null -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun displayTenantDetail(tenant: TenantResponse) {
        currentTenant = tenant
        binding.apply {
            tvTenantName.text = tenant.fullName
            tvPhone.text = tenant.phone?.takeIf { it.isNotBlank() } ?: "Chưa có SĐT"
            tvEmail.text = tenant.email ?: "Chưa có email"
            tvIdentity.text = "CMND/CCCD: ${tenant.identityNumber ?: "Chưa thu thập"}"
            tvDateOfBirth.text = "Ngày sinh: ${tenant.dateOfBirth ?: "Chưa có"}"
            tvAddress.text = "Địa chỉ: ${tenant.address ?: "Chưa có"}"
            tvEmergencyContact.text = "Liên hệ khẩn cấp: ${tenant.emergencyContact ?: "Chưa có"}"
            tvPropertyName.text = "Nhà trọ: ${tenant.propertyName}"
            tvCurrentRoom.text = "Phòng: ${tenant.currentRoom ?: "Chưa thuê phòng"}"

            val statusLabel = when (tenant.accountStatus) {
                "ACTIVE" -> "Tài khoản: Đã kích hoạt"
                "PENDING_ACTIVATION" -> "Tài khoản: Chờ kích hoạt OTP"
                else -> "Tài khoản: Chưa tạo"
            }
            tvAccountStatus.text = statusLabel
            btnProvisionAccount.visibility =
                if (tenant.accountStatus == "ACTIVE") View.GONE else View.VISIBLE
            btnResendActivation.visibility =
                if (tenant.accountStatus == "PENDING_ACTIVATION") View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
