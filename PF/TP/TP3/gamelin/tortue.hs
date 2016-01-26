module Tortue where
import           Graphics.Gloss

type EtatTortue = (Point, Float)
type Symbole  = Char
type Mot      = [Symbole]

type Config = (EtatTortue -- État initial de la tortue
              ,Float      -- Longueur initiale d’un pas
              ,Float      -- Facteur d’échelle
              ,Float      -- Angle pour les rotations de la tortue
              ,[Symbole]) -- Liste des symboles compris par la tortue

type EtatDessin = (EtatTortue, Path)

etatInitial:: Config -> EtatTortue
etatInitial (etatTortue, _, _, _, _) = etatTortue

longueurPas:: Config -> Float
longueurPas (_, longueur_pas, _, _, _) = longueur_pas

facteurEchelle:: Config -> Float
facteurEchelle (_, _, facteur_echelle, _, _) = facteur_echelle

angle:: Config -> Float
angle (_, _, _, a, _) = a

symbolesTortue:: Config -> [Symbole]
symbolesTortue (_, _, _, _, symboles_tortue) = symboles_tortue

avance :: Config -> EtatTortue -> EtatTortue
avance c ((x, y), cap) = ((x', y'), cap)
        where
            x' = x + longueurPas c * cos cap
            y' = y + longueurPas c * sin cap

tourneAGauche:: Config -> EtatTortue -> EtatTortue
tourneAGauche c ((_, _), cap) = ( (_,_), cap')
        where
            cap' = cap + angle c

tourneADroite:: Config -> EtatTortue -> EtatTortue
tourneADroite  c ((_, _), cap) = ( (_,_), cap')
        where
            cap' = cap - angle c

-- Définissez la fonction filtreSymbolesTortue, qui supprime tous les symboles qui ne sont pas des ordres pour la tortue dans le mot passé en argument.
filtreSymbolesTortue :: Config -> Mot -> Mot
filtreSymbolesTortue c m = [s | s <- m, s `elem` symbolesTortue c]

-- Définissez une fonction interpreteSymbole, qui calcule le nouvel état atteint par l’exécution de l’ordre correspondant au symbole donné en partant de l’état donné.
interpreteSymbole :: Config -> EtatDessin -> Symbole -> EtatDessin
interpreteSymbole c (etat, path) s
      | s == 'F' = (eAvance, dAvance)
      | s == '+' = (eTourneAGauche, dTourneAGauche)
      | s == '-' = (eTourneADroite, dTourneADroite)
      | otherwise = error "Symbole not match"
      where eAvance = avance c etat
            eTourneADroite = tourneADroite c etat
            eTourneAGauche = tourneAGauche c etat
            dAvance = path ++ [fst eAvance]
            dTourneADroite = path ++ [fst eTourneADroite]
            dTourneAGauche = path ++ [fst eTourneAGauche]

interpreteMot :: Config -> Mot -> Picture
interpreteMot c m = line (snd (foldl (\ e s -> interpreteSymbole c e s) initial_Etat mot_filter))
  where initial_path = fst (etatInitial c)
        initial_Etat = (etatInitial c, [initial_path])
        mot_filter = filtreSymbolesTortue c m


dessin = interpreteMot (((-150,0),0),100,1,pi/3,"F+-") "F+F--F+F"
main = display (InWindow "L-système" (1000, 1000) (0, 0)) white dessin
