module proyectopos.restauranteappfrontend {
    // JavaFX Modules requeridos para la aplicación base
    requires javafx.controls;
    requires javafx.fxml;

    // Módulos requeridos para las librerías extra (BootstrapFX, ControlsFX, etc.)
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    // Gson (para serializar/deserializar JSON)
    requires com.google.gson;

    // HttpClient (para llamadas HTTP)
    requires java.net.http;

    // --- Apertura de paquetes para JavaFX (Reflexión) ---
    // Permite a JavaFX acceder a los controladores y vistas
    opens proyectopos.restauranteappfrontend to javafx.fxml;
    opens proyectopos.restauranteappfrontend.controllers to javafx.fxml;

    // --- Apertura de paquetes para Gson (Reflexión) ---
    // Permite a Gson acceder a los campos privados de tus clases modelo y DTO
    opens proyectopos.restauranteappfrontend.model to com.google.gson;
    opens proyectopos.restauranteappfrontend.model.dto to com.google.gson; // <-- Corrección añadida

    // --- Exportación de paquetes (Visibilidad) ---
    // Hace visibles las clases públicas de estos paquetes para otros módulos
    exports proyectopos.restauranteappfrontend;
    exports proyectopos.restauranteappfrontend.controllers;
    exports proyectopos.restauranteappfrontend.model;
    exports proyectopos.restauranteappfrontend.model.dto; // <-- Corrección añadida
    exports proyectopos.restauranteappfrontend.services; // <-- Añadido (buena práctica)
    exports proyectopos.restauranteappfrontend.util;    // <-- Añadido (buena práctica)
}