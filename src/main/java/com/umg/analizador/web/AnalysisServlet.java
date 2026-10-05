package com.umg.analizador.web;

import com.umg.analizador.service.AnalysisResult;
import com.umg.analizador.service.AnalysisService;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "AnalysisServlet", urlPatterns = "/api/analyze")
public class AnalysisServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AnalysisServlet.class.getName());

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        if (!"POST".equals(request.getMethod())) {
            response.setHeader("Allow", "POST");
            write(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    AnalysisJson.error("Método no permitido. Utiliza POST."));
            return;
        }
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Debe establecerse antes de que el contenedor decodifique los parámetros.
        request.setCharacterEncoding("UTF-8");
        String contentType = request.getContentType();
        if (contentType == null || !"application/x-www-form-urlencoded".equals(
                contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT))) {
            write(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    AnalysisJson.error("Envía codigoRuby como formulario de texto."));
            return;
        }

        String json;
        try {
            String codigoRuby = request.getParameter("codigoRuby");
            if (codigoRuby == null) {
                write(response, HttpServletResponse.SC_BAD_REQUEST,
                        AnalysisJson.error("Falta el parámetro codigoRuby."));
                return;
            }
            // El texto vacío conserva la conducta natural de AnalysisService.
            json = AnalysisJson.serialize(analyze(codigoRuby));
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "No fue posible completar el análisis", exception);
            write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    AnalysisJson.error("No fue posible completar el análisis."));
            return;
        }
        write(response, HttpServletResponse.SC_OK, json);
    }

    protected AnalysisResult analyze(String codigoRuby) {
        return new AnalysisService().analyze(codigoRuby);
    }

    private static void write(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.getWriter().write(json);
    }
}
