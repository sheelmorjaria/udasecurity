module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires guava;
    requires java.desktop;
    requires com.google.gson;
    requires java.prefs;
    requires miglayout.swing;
    opens com.udacity.catpoint.security.data to com.google.gson;


}