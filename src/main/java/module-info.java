module proyectopos.restauranteappfrontend {
    // JavaFX Modules requeridos para la aplicación base
    requires javafx.controls;
    requires javafx.fxml;

    // Módulos requeridos para las librerías extra (BootstrapFX, ControlsFX, etc.)
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    // 🛑 ARREGLO CLAVE 1: Gson (para serializar JSON)
    requires com.google.gson;

    // 🛑 ARREGLO CLAVE 2: HttpClient (para llamadas HTTP)
    // El módulo de red es parte del JDK, pero debe ser requerido.
    requires java.net.http;

    // Se asegura de que JavaFX pueda inyectar el controlador y acceder a sus elementos
    opens proyectopos.restauranteappfrontend to javafx.fxml;
    exports proyectopos.restauranteappfrontend;

    // Asegura que JavaFX pueda inyectar los controladores
    opens proyectopos.restauranteappfrontend.controllers to javafx.fxml;
    exports proyectopos.restauranteappfrontend.controllers;

    // Exponer el modelo para que Gson pueda acceder a él si es necesario (buena práctica)
    exports proyectopos.restauranteappfrontend.model;

    opens proyectopos.restauranteappfrontend.model to com.google.gson;
}
