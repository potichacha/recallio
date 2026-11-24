import { Link } from 'react-router'
import { legalConfig } from './legalConfig'
import { ContactEmail, LegalList, LegalPage, LegalSection } from './LegalText'
import { TEXT_LINK } from './textLink'

/** À changer à chaque modification du texte : c'est la version que le lecteur a lue. */
const UPDATED_ON = '24 novembre 2026'

/**
 * Les mentions légales (ADR 0074) : qui édite, qui héberge, comment écrire. Le nom de l'éditeur et son adresse de
 * contact viennent de la configuration du conteneur (`legalConfig`), l'hébergeur est un fait public. Sans nom,
 * l'éditeur use de la faculté que la loi laisse à un particulier : n'être connu que de son hébergeur.
 */
export function LegalNoticePage() {
  const { publisher, contactEmail } = legalConfig()
  return (
    <LegalPage title="Mentions légales" updatedOn={UPDATED_ON}>
      <LegalSection title="L’éditeur">
        {publisher ? (
          <p>
            Recallio est édité par <strong className="text-ink">{publisher}</strong>, un
            particulier, à titre non professionnel. C’est aussi le directeur de la publication.
          </p>
        ) : (
          <>
            <p>
              Recallio est édité par un particulier, à titre non professionnel. C’est aussi le
              directeur de la publication.
            </p>
            <p>
              Comme la loi le permet à un éditeur non professionnel, il ne publie pas son nom : il a
              communiqué son identité à l’hébergeur du site, nommé ci-dessous (loi n° 2004-575 du 21
              juin 2004 pour la confiance dans l’économie numérique, article 1-1, II).
            </p>
          </>
        )}
      </LegalSection>

      <LegalSection title="L’hébergeur">
        <p>
          Le site et ses données sont hébergés par OVH SAS, 2 rue Kellermann, 59100 Roubaix, France
          (
          <a href="https://www.ovhcloud.com" rel="noopener noreferrer" className={TEXT_LINK}>
            ovhcloud.com
          </a>
          ).
        </p>
      </LegalSection>

      <LegalSection title="Nous écrire">
        <p>
          Pour une question, pour exercer tes droits sur tes données ou pour signaler un contenu :{' '}
          <ContactEmail email={contactEmail} />.
        </p>
      </LegalSection>

      <LegalSection title="Un service en bêta">
        <LegalList>
          <li>
            Recallio est gratuit et en bêta : il est fourni tel quel, sans garantie de
            disponibilité. Il peut être interrompu, changer ou s’arrêter.
          </li>
          <li>
            Les cours, les cartes et les exercices sont rédigés avec l’aide d’une intelligence
            artificielle, puis relus par l’éditeur. Il peut y rester des erreurs.
          </li>
          <li>
            Recallio t’aide à réviser : il ne garantit ni l’exactitude de chaque réponse, ni la
            réussite d’un entretien.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="Signaler un contenu">
        <LegalList>
          <li>
            Une erreur dans une carte ou dans une page du Tour : « Signaler une erreur », sous
            chaque carte et sur chaque page, une fois connecté.
          </li>
          <li>
            Un contenu que tu estimes illicite (un nom affiché, un nom de groupe, l’adresse d’un
            profil public) : écris à <ContactEmail email={contactEmail} />, en donnant l’adresse de
            la page et ce qui pose problème.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="Propriété intellectuelle">
        <LegalList>
          <li>
            Le code source de Recallio est public. Il n’est accompagné d’aucune licence : tu peux le
            lire, mais le réutiliser demande l’accord de l’éditeur.
          </li>
          <li>
            Les textes des cours, des cartes et des exercices, le nom Recallio et la mascotte Rico
            sont ceux du projet. Les reproduire demande aussi l’accord de l’éditeur.
          </li>
          <li>
            Les noms et les logos des langages et des outils présentés appartiennent à leurs
            propriétaires : ils ne servent ici qu’à désigner ces technologies.
          </li>
        </LegalList>
      </LegalSection>

      <LegalSection title="Tes données">
        <p>
          Ce que Recallio garde sur toi, pourquoi, combien de temps, et comment le récupérer ou
          l’effacer : c’est dans la{' '}
          <Link to="/confidentialite" className={TEXT_LINK}>
            politique de confidentialité
          </Link>
          .
        </p>
      </LegalSection>
    </LegalPage>
  )
}
