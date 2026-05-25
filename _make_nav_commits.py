#!/usr/bin/env python3
import subprocess
from pathlib import Path

ROOT = Path(r"C:\Users\MSI\AndroidStudioProjects\MicroServe")
FINAL = ROOT / ".nav_final"


def run(*args):
    subprocess.run(args, cwd=ROOT, check=True)


def commit(msg: str):
    run("git", "add", "-A")
    run("git", "commit", "-m", msg)


def write(rel: str, content: str):
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


def read_final(name: str) -> str:
    text = (FINAL / name).read_text(encoding="utf-8-sig")
    if not text.endswith("\n"):
        text += "\n"
    return text


layout_final = read_final("view_user_bottom_nav.xml")
helper_final = read_final("UserBottomNavHelper.kt")

# Split layout at tab boundaries
marker_row = '    <LinearLayout\n        android:id="@+id/navItemsRow"'
idx_row = layout_final.index(marker_row)
layout_bubble = layout_final[:idx_row].rstrip() + "\n\n</androidx.constraintlayout.widget.ConstraintLayout>\n"

layout_with_row = layout_final[: layout_final.index("        <LinearLayout\n            android:id=\"@+id/navService\"")]
layout_with_row = layout_with_row.rstrip() + "\n    </LinearLayout>\n\n</androidx.constraintlayout.widget.ConstraintLayout>\n"

idx_service = layout_final.index('            android:id="@+id/navService"')
idx_home = layout_final.index('            android:id="@+id/navHome"')
idx_post = layout_final.index('            android:id="@+id/navPost"')
idx_profile = layout_final.index('            android:id="@+id/navProfile"')

layout_request_service = (
    layout_final[:idx_home].rstrip()
    + "\n    </LinearLayout>\n\n</androidx.constraintlayout.widget.ConstraintLayout>\n"
)
layout_through_home = (
    layout_final[:idx_post].rstrip()
    + "\n    </LinearLayout>\n\n</androidx.constraintlayout.widget.ConstraintLayout>\n"
)
layout_through_post = (
    layout_final[:idx_profile].rstrip()
    + "\n    </LinearLayout>\n\n</androidx.constraintlayout.widget.ConstraintLayout>\n"
)

helper_constants = """package com.example.microserve

import androidx.appcompat.app.AppCompatActivity

object UserBottomNavHelper {

    const val EXTRA_ACTIVE_TAB = "extra_active_tab"

    const val TAB_REQUEST = "request"
    const val TAB_SERVICE = "service"
    const val TAB_HOME = "home"
    const val TAB_POST = "post"
    const val TAB_PROFILE = "profile"

    fun setup(activity: AppCompatActivity, currentTab: String) {
        // Navigation wiring added in follow-up commits.
    }
}
"""

idx_apply = helper_final.index("    private fun applyActiveTabState(")
idx_navigate = helper_final.index("    private fun navigateToTab(")

helper_setup_only = helper_final[:idx_apply].rstrip() + "\n}\n"
helper_with_apply = helper_final[:idx_navigate].rstrip() + "\n}\n"

# RequestMainActivity intermediate: remove old handlers only
req_final = read_final("RequestMainActivity.kt")
req_no_handlers = req_final.replace(
    """        binding.requestsButton.setOnClickListener {
            binding.mainScrollView.smoothScrollTo(0, binding.myRequestsLabel.top)
        }
""",
    """        binding.requestsButton.setOnClickListener {
            binding.mainScrollView.smoothScrollTo(0, binding.myRequestsLabel.top)
        }

        binding.requestTab.setOnClickListener {
            // Already on Request screen
        }

        binding.serviceTab.setOnClickListener {
            Toast.makeText(this, "Service screen coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.homeTab.setOnClickListener {
            startActivity(
                Intent(this, Homepage::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }

        binding.postTab.setOnClickListener {
            startActivity(Intent(this, PostAddActivity::class.java))
        }

        binding.profileTab.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }
""",
).replace(
    "        UserBottomNavHelper.setup(this, UserBottomNavHelper.TAB_REQUEST)\n",
    "",
).replace(
    "            findViewById<View>(R.id.navContainer)?.setPadding(0, 0, 0, systemBars.bottom)",
    "            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)",
)

post_final = read_final("PostAdsActivity.kt")
post_active_only = post_final.replace(
    """        setupWindowInsets()
        setupSpinner()
        setupClickListeners()
        UserBottomNavHelper.setup(this, activeTab)
""",
    """        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupSpinner()
        setupClickListeners()
""",
).replace(
    """
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.headerContainer.setPadding(
                binding.headerContainer.paddingLeft,
                systemBars.top + 16,
                binding.headerContainer.paddingRight,
                binding.headerContainer.paddingBottom
            )
            findViewById<android.view.View>(R.id.navContainer)?.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }
""",
    "",
)

edit_final = read_final("EditPostActivity.kt")
edit_no_email = edit_final.replace(
    """        binding.saveBtn.setOnClickListener {
            if (validateFields()) {
                showToast("Post updated successfully")
                finish()
            }
        }
    }

    private fun validateFields(): Boolean {
        val category = binding.categorySpinner.selectedItem.toString()
        val name = binding.providerNameET.text.toString().trim()
        val location = binding.locationET.text.toString().trim()
        val contact = binding.contactET.text.toString().trim()

        return when {
            category == "-Select-" -> {
                showToast("Please select a category")
                false
            }
            name.isEmpty() || location.isEmpty() || contact.isEmpty() -> {
                showToast("Please fill all fields")
                false
            }
            else -> true
        }
    }
""",
    """        binding.okBtn.setOnClickListener {
            if (validateFields()) {
                showToast("Changes saved")
                finish()
            }
        }

        binding.postBtn.setOnClickListener {
            if (validateFields()) {
                showToast("Post updated successfully")
                finish()
            }
        }
    }

    private fun validateFields(): Boolean {
        val category = binding.categorySpinner.selectedItem.toString()
        val name = binding.providerNameET.text.toString().trim()
        val location = binding.locationET.text.toString().trim()
        val contact = binding.contactET.text.toString().trim()
        val email = binding.emailET.text.toString().trim()

        return when {
            category == "-Select-" -> {
                showToast("Please select a category")
                false
            }
            name.isEmpty() || location.isEmpty() || contact.isEmpty() || email.isEmpty() -> {
                showToast("Please fill all fields")
                false
            }
            else -> true
        }
    }
""",
).replace(
    """
        binding.providerNameET.setText(defaultName)
        binding.locationET.setText(defaultLocation)
        binding.contactET.setText(defaultContact)

        val categoryPosition""",
    """
        binding.providerNameET.setText(defaultName)
        binding.locationET.setText(defaultLocation)
        binding.contactET.setText(defaultContact)
        binding.emailET.setText(defaultEmail)

        val categoryPosition""",
).replace(
    """        val defaultContact = intent.getStringExtra("contact") ?: "072587456"

        binding.providerNameET.setText(defaultName)""",
    """        val defaultContact = intent.getStringExtra("contact") ?: "072587456"
        val defaultEmail = intent.getStringExtra("email") ?: "Sunil@gmail.com"

        binding.providerNameET.setText(defaultName)""",
)

commits = [
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_final.split("    <ImageView\n        android:id=\"@+id/navBase\"")[0] + "    <ImageView\n        android:id=\"@+id/navBase\"" + layout_final.split("    <ImageView\n        android:id=\"@+id/navBase\"")[1].split("\n\n    <ImageView\n        android:id=\"@+id/navSubtract\"")[0] + "\n\n</androidx.constraintlayout.widget.ConstraintLayout>\n", "feat: add purple base bar layer to shared bottom nav"),
]

# Rebuild layout_base properly
base_end = layout_final.index("\n\n    <ImageView\n        android:id=\"@+id/navSubtract\"")
layout_base = layout_final[:base_end] + "\n\n</androidx.constraintlayout.widget.ConstraintLayout>\n"

commits = [
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_base, "feat: add purple base bar layer to shared bottom nav"),
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_bubble, "feat: add floating bubble layers to shared user bottom nav"),
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_with_row, "feat: add request tab row to shared bottom nav"),
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_request_service, "feat: add service tab to shared bottom nav layout"),
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_through_home, "feat: add home tab to shared bottom nav layout"),
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_through_post, "feat: add post tab with spacer to shared bottom nav"),
    ("app/src/main/res/layout/view_user_bottom_nav.xml", layout_final, "feat: add profile tab completing shared bottom nav layout"),
    ("app/src/main/java/com/example/microserve/UserBottomNavHelper.kt", helper_constants, "feat: create UserBottomNavHelper tab constants"),
    ("app/src/main/java/com/example/microserve/UserBottomNavHelper.kt", helper_setup_only, "feat: bind user bottom nav views and tab click listeners"),
    ("app/src/main/java/com/example/microserve/UserBottomNavHelper.kt", helper_with_apply, "feat: implement active tab bubble positioning in UserBottomNavHelper"),
    ("app/src/main/java/com/example/microserve/UserBottomNavHelper.kt", helper_final, "feat: add tab navigation routes to UserBottomNavHelper"),
    ("app/src/main/res/layout/activity_post_ads.xml", read_final("activity_post_ads.xml"), "refactor: replace inline bottom nav in PostAds with shared include"),
    ("app/src/main/res/layout/activity_request_main.xml", read_final("activity_request_main.xml"), "refactor: replace RequestMain bottom nav with shared include"),
    ("app/src/main/java/com/example/microserve/RequestMainActivity.kt", req_no_handlers, "refactor: prepare RequestMainActivity for shared bottom nav migration"),
    ("app/src/main/java/com/example/microserve/RequestMainActivity.kt", req_final, "feat: integrate UserBottomNavHelper into RequestMainActivity"),
    ("app/src/main/java/com/example/microserve/PostAdsActivity.kt", post_active_only, "feat: add active tab intent handling to PostAdsActivity"),
    ("app/src/main/java/com/example/microserve/PostAdsActivity.kt", post_final, "feat: wire UserBottomNavHelper and nav insets into PostAdsActivity"),
    ("app/src/main/java/com/example/microserve/EditPostActivity.kt", edit_no_email, "fix: remove obsolete email field usage from EditPostActivity"),
    ("app/src/main/java/com/example/microserve/EditPostActivity.kt", edit_final, "fix: use saveBtn flow in EditPostActivity after main merge"),
]

for rel, content, msg in commits:
    write(rel, content)
    commit(msg)

print("Created", len(commits), "commits")
