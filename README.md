# Loady 
Fluent syntax library for attaching progress indicators to ViewGroups.

## Example
```kotlin
// show custom loading layout
Loady.of(container).with(R.layout.loading_layout).show()

// show custom loading widget
Loady.of(container).with(CustomWidget(this)).show()

// hide indicator from the container
Loady.of(container).hide()

```

Shows a loading indicator inside of the ```container```. Container could be any ViewGroup, eg FrameLayout, RelativeLayout etc.

Extending the `Widget` class allows you to build more sophisticated progress indicators.

Project contains a sample application.

## Adding to your project

```java
repositories {
  maven { url "https://jitpack.io" }
}

dependencies {
  implementation 'com.github.rafalzawadzki:Loady:1.00'
}

```
