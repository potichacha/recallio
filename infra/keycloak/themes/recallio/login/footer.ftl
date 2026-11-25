<#--
  Le pied de chaque page du thème (ADR 0075) : les liens vers les pages légales de l'application (ADR 0074).

  C'est le seul gabarit du thème. Le thème parent (keycloak.v2) l'appelle depuis template.ftl, au bas de la carte, et
  le laisse vide pour qu'un thème le remplisse : il ne recopie donc rien du parent. À une montée de version de
  Keycloak, vérifier que template.ftl importe toujours footer.ftl et appelle sa macro « content », et que « pageId »
  vaut toujours « register » sur la page d'inscription ; le parcours e2e « keycloak-theme » échoue sinon.

  L'origine publique vient de l'environnement du conteneur (theme.properties). Tant qu'elle n'est pas une adresse,
  aucun lien n'est écrit.
-->
<#macro content>
  <#local origin = properties.recallioPublicOrigin!"">
  <#if origin?starts_with("http")>
    <#if (pageId!"") == "register">
      <p id="recallio-legal" class="recallio-legal">
        ${msg("recallioLegalRegisterLead")}
        <a href="${origin}/confidentialite">${msg("recallioPrivacyPolicyInSentence")}</a>
        ${msg("recallioLegalAnd")}
        <a href="${origin}/mentions-legales">${msg("recallioLegalNoticeInSentence")}</a>.
      </p>
    <#else>
      <nav id="recallio-legal" class="recallio-legal" aria-label="${msg("recallioLegalNav")}">
        <a href="${origin}/mentions-legales">${msg("recallioLegalNotice")}</a>
        <a href="${origin}/confidentialite">${msg("recallioPrivacyPolicy")}</a>
      </nav>
    </#if>
  </#if>
</#macro>
