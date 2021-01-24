
# @r4dic4l/react-native-open-doc

This is a fork of [react-native-open-doc](https://github.com/capriza/react-native-open-doc).

Open files stored on device for preview - Android and iOS. 

Pick files using native file pickers for iOS and Android (UIDocumentPickerViewController / Intent.ACTION_OPEN_DOCUMENT)

Share files on Android (for iOS use the react-native Share.share({ url: selectedUri }) api, see https://reactnative.dev/docs/share#share for more details).

## Getting started

`$ npm install @r4dic4l/react-native-open-doc --save`

### Mostly automatic installation

`$ react-native link @r4dic4l/react-native-open-doc`

Android ONLY:
  [Define a FileProvider](https://developer.android.com/reference/android/support/v4/content/FileProvider)
  
  Define a file provider in your AndroidManifest.xml.
  Note that the authorities value should be `<your package name>.provider`, for example:

  ```xml
  <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="com.mydomain.provider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data
          android:name="android.support.FILE_PROVIDER_PATHS"
          android:resource="@xml/provider_paths" />
  </provider>
  ```

  In meta-data, all supported files should be listed. @xml/provider_paths takes values configured in xml/provider_paths.xml.

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <paths xmlns:android="http://schemas.android.com/apk/res/android">
    <files-path name="shared" path="."/>
    <external-path name="shared" path="."/>
    <external-files-path name="shared" path="."/>
    <root-path name="root" path="."/>
  </paths>
  ```

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-open-doc` and add `RNCOpenDoc.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCOpenDoc.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.capriza.reactlibrary.RNCOpenDocPackage;` to the imports at the top of the file
  - Add `new RNCOpenDocPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-open-doc'
  	project(':react-native-open-doc').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-open-doc/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      implementation project(':react-native-open-doc')
  	```
4. Define a FileProvider

## Usage
```javascript
import RNCOpenDoc from '@r4dic4l/react-native-open-doc';

// For opening a document by path (android and iOS)
// Works with file:// prefix OR direct file path
RNCOpenDoc.open(pathToFile);
// For opening a content:// document (android ONLY)
// Works with file:// prefix OR direct file path OR content:// URI string with a suggested mime type for intent
RNCOpenDoc.openWithSuggestedMime(contentUri, suggestedMimeType);

// For sharing a document by path (android ONLY)
// For iOS use the react-native Share.share({ url: selectedUri }) api (see https://reactnative.dev/docs/share#share)
// Works with file:// prefix OR direct file path
RNCOpenDoc.share(pathToFile);
// For sharing a content:// document (android ONLY)
// Works with file:// prefix OR direct file path OR content:// URI string with a suggested mime type for intent
RNCOpenDoc.shareWithSuggestedMime(pathToFile, suggestedMimeType);

// For using the file picker (android and iOS)
RNCOpenDoc.pick(null, (error, files) => {
    if (error) {
        console.log(`error in RNCOpenDoc.pick ${error}`);
    }
    else if (files) {
    	this.handleSelectedFiles(files);
    }
});
```
 
`files` is an array of objects with the following properties:

- `fileName` (string) e.g. "foo.html"
- `fileSize` (number) (iOS only) File size in bytes
- `mimeType` (string) (iOS only) e.g. "text/html"
- `uri` (string) Example (iOS): "file:///private/var/mobile/Containers/Data/Application/.../foo.html"

