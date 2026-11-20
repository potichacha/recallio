package com.recallio.content.api;

import com.recallio.content.application.SitemapService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Le plan du site (ADR 0065), au format de sitemaps.org : nginx le sert sous {@code /sitemap.xml}, et robots.txt y
 * renvoie. Public, comme le catalogue. Chaque page n'a que son adresse ({@code <loc>}) : Google ignore
 * {@code changefreq} et {@code priority}, et aucune date fiable ne donnerait {@code lastmod}.
 */
@Path("/api/v1/sitemap.xml")
@Tag(name = "Contenu", description = "Sujets, thèmes et cartes publiés")
public class SitemapResource {

    static final String NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";

    /** Les moteurs relisent le plan de temps en temps, et le catalogue change rarement : une heure de cache. */
    static final String CACHE_CONTROL = "public, max-age=3600";

    private final SitemapService sitemap;

    /** L'origine publique du site, sans barre finale : les adresses d'un plan du site sont absolues. */
    private final String publicOrigin;

    public SitemapResource(
            SitemapService sitemap, @ConfigProperty(name = "recallio.public-origin") String publicOrigin) {
        this.sitemap = sitemap;
        this.publicOrigin =
                publicOrigin.endsWith("/") ? publicOrigin.substring(0, publicOrigin.length() - 1) : publicOrigin;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Operation(
            operationId = "getSitemap",
            summary = "Le plan du site : les pages publiques des sujets publiés, pour les moteurs de recherche")
    @APIResponse(
            responseCode = "200",
            description = "Le plan du site, au format de sitemaps.org",
            content = @Content(mediaType = MediaType.APPLICATION_XML, schema = @Schema(type = SchemaType.STRING)))
    public Response getSitemap() {
        return Response.ok(write(sitemap.paths()))
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                .build();
    }

    /** Écrit le plan avec le JDK : l'écrivain échappe ce qu'une adresse pourrait contenir de réservé en XML. */
    private String write(List<String> paths) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(out);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("urlset");
            xml.writeDefaultNamespace(NAMESPACE);
            for (String path : paths) {
                xml.writeStartElement("url");
                xml.writeStartElement("loc");
                xml.writeCharacters(publicOrigin + path);
                xml.writeEndElement();
                xml.writeEndElement();
            }
            xml.writeEndElement();
            xml.writeEndDocument();
            xml.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Impossible d'écrire le plan du site", e);
        }
        return out.toString();
    }
}
