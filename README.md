📋 Prerequisites
Before you begin, ensure you have the following installed:

Android Studio: Arctic Fox 2020.3.1 or higher

Android SDK: API Level 24 (Android 7.0) or higher

Java JDK: Version 11 or higher

EnableX Account: You'll need a virtual number from EnableX

🚀 Installation

Clone the repository:

bash
git clone https://github.com/EnableX/Gen_AI_Voice_Android.git
cd Gen_AI_Voice_Android
Open the project in Android Studio:

Launch Android Studio.

Select "Open" and navigate to the cloned project folder.

Click "OK" to open the project.

Configure your virtual number:

Open app/src/main/java/com/enablex/genaivoiceandroid/MainActivity.kt (or .java).

Update the virtual number variable:

kotlin
private val virtualNumber = "your_virtual_number_here"
Build and run the app:

Connect an Android device or start an emulator.

Click "Run" → "Run 'app'" or press Shift+F10.

⚙️ Configuration
Permissions Setup
The app requires the following permissions, which are already configured in AndroidManifest.xml:

xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
EnableX SDK Integration
Add the EnableX repository and dependency in app/build.gradle (Note: The original text mentions .AAR file import but does not show the code. Ensure the Gradle snippet is included here.).

EnableX Token Endpoint
The app fetches tokens from your endpoint. Ensure your virtual number is registered with EnableX and that the token endpoint is accessible.

text
Enter your endpoint to fetch the token.
📱 Usage

Launch the app. The voice agent screen will be displayed.

Get Started. Tap the "Get Started" button to initiate the connection.

Grant permissions. Allow microphone access when prompted.

Wait for connection. The status will show "CONNECTING..." and then "CONNECTED".

Interact with the agent. Speak naturally; the app indicates when you or the bot is speaking.

Mute/Unmute. Use the microphone button to toggle your audio.

Disconnect. Tap "Disconnect" to end the call.

📦 Dependencies
The app uses the following key dependencies:

EnableX Voice Bot SDK: For real-time voice interaction.

Retrofit: A type-safe HTTP client for API calls.

Material Components: UI components following Material Design.

🎨 UI Features

Animated Speaking Indicators: Visual feedback when you or the bot is speaking.

Connection Status: Real-time connection state display.

Control Buttons:

Connect/Disconnect button with state changes.

Mute/Unmute toggle button (visible when connected).

Responsive Layout: Adapts to different screen sizes and orientations.

🔧 Troubleshooting
Connection Issues

Verify the virtual number is correctly configured in MainActivity.

Check the internet connectivity on your device.

Ensure the EnableX token endpoint returns valid tokens.

Verify the virtual number is active in the EnableX portal.

Permission Issues

Ensure microphone permission is granted.

Check app settings if permissions were denied.

For Android 6.0+, runtime permissions are handled automatically.

Build Issues

Clean and rebuild the project: Build → Clean Project, then Build → Rebuild Project.

Invalidate caches: File → Invalidate Caches and Restart.

Ensure the correct Java version is configured.

Check that Gradle sync completes successfully.

SDK Issues

Verify the EnableX SDK version is compatible.

Check ProGuard rules if using minification.

Ensure all required permissions are declared.

📱 Device Requirements

Minimum Android Version: Android 7.0 (API Level 24)

Recommended Android Version: Android 10.0 (API Level 29) or higher

Hardware Requirements:

Microphone

Speaker or headphones

Stable internet connection

🔐 Security Notes

Never hardcode sensitive data in source files.

Use secure credential storage for production.

Implement proper certificate pinning for production deployments.

Follow Android security best practices for network calls.

📝 License
This project is a sample application. Please refer to EnableX's terms of service for SDK usage.

📞 Support

For issues related to the EnableX SDK, contact the EnableX support team.

For app issues, open an issue in the GitHub repository.

🙏 Acknowledgments

EnableX for providing the Voice Bot SDK.

The Android developer community for resources and libraries.

Note: This is a sample application for demonstration purposes. For production use, implement proper error handling, security measures, and compliance with relevant regulations.