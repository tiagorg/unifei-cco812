package componentes.modelo.formas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Classe que representa um Poligono
 */
public class Poligono extends Figura {
	private List<Ponto> listaVertices;
	private int passo;

	public Poligono(List<Ponto> listaVertices, int passo) {
		this.listaVertices = listaVertices;
		this.passo = passo;

		List<Segmento> listaSegmentos = constroiBordas();
		preenchePoligono(listaSegmentos);

		listaFormasPlotaveis.addAll(listaSegmentos);
	}

	/**
	 * Preenche um poligono com Scan Lines
	 * @param listaSegmentos
	 */
	@SuppressWarnings("unchecked")
	private void preenchePoligono(List<Segmento> listaSegmentos) {
		// Declaracao de variaveis
		TreeMap<Integer, List<Borda>> tabelaBordas, tabelaScanLines;
		Map.Entry<Integer, List<Borda>> cabeca;
		List<Borda> listaBordas;
		Queue<Borda> filaCalculo;
		Borda borda, borda2;
		Integer chave;
		int tamanhoFila, i, linhaAtual;
		Double incremento;
		Segmento segmentoBorda = null;
		Segmento[] segmento;

		// Constroi as bordas e associa aos segmentos
		for (Segmento seg: listaSegmentos) {
			chave = seg.getPInicial().getY();
			incremento = seg.getIncremento();

			if (incremento.isInfinite() == false)
				seg.setBorda(new Borda(seg.getPFinal().getY(),	(double) seg.getPInicial().getX(), incremento));
		}
		
		// Varre os segmentos e adiciona a tabela de bordas, ajustando
		tabelaBordas = new TreeMap<Integer, List<Borda>>();
		
		// Constroi os segmentos a partir dos vertices
		for (i = 0; i < listaSegmentos.size(); i++) {
			segmento = new Segmento[2];
			
			segmento[0] = listaSegmentos.get(i);
			if (i == listaSegmentos.size()-1) {
				segmento[1] = listaSegmentos.get(0);
			}
			else {
				segmento[1] = listaSegmentos.get(i+1);
			}
			
			// Indica ajuste
			int indiceAjustar = necessitaAjuste(segmento[0], segmento[1]);
			switch(indiceAjustar){
			case 0:
			case 1:
				if (segmento[indiceAjustar].getBorda() != null)
					segmento[indiceAjustar].getBorda().setNecessitaAjuste(passo);
				break;
			}
			
			// Ajusta a volta
			if (i == listaSegmentos.size()-1) {
				if (indiceAjustar != -1 && segmento[indiceAjustar].getBorda() != null && 
						segmento[indiceAjustar].getBorda().getIncremento().isInfinite() == false &&
						segmento[indiceAjustar].getBorda().necessitaAjuste()) {
					chave = segmento[indiceAjustar].getPInicial().getY();
					listaBordas = tabelaBordas.get(chave);
					
					if (listaBordas != null) 
						listaBordas.remove(segmento[indiceAjustar].getBorda());
						
					chave = segmento[indiceAjustar].getPInicial().getY()+ passo;
					
					if (tabelaBordas.containsKey(chave) == false) {
						tabelaBordas.put(chave, new ArrayList<Borda>());
					}
					listaBordas = tabelaBordas.get(chave);
					listaBordas.add(segmento[indiceAjustar].getBorda());
				}
				else {
					if (segmento[0].getBorda().necessitaAjuste()) {
						chave = segmento[0].getPInicial().getY()+passo;	
					}
					else {
						chave = segmento[0].getPInicial().getY();
					}
					if (tabelaBordas.containsKey(chave) == false) {
						tabelaBordas.put(chave, new ArrayList<Borda>());
					}
					listaBordas = tabelaBordas.get(chave);
					listaBordas.add(segmento[0].getBorda());
				}
				System.out.println();
				continue;
			}
			
			// Insere borda na tabela, ajustando 
			if (segmento[0].getBorda() == null || segmento[0].getBorda().getIncremento().isInfinite()) continue;
			
			if (segmento[0].getBorda().necessitaAjuste()) {
				chave = segmento[0].getPInicial().getY()+passo;	
			}
			else {
				chave = segmento[0].getPInicial().getY();
			}
			
			if (tabelaBordas.containsKey(chave) == false) {
				tabelaBordas.put(chave, new ArrayList<Borda>());
			}
			listaBordas = tabelaBordas.get(chave);
			listaBordas.add(segmento[0].getBorda());
			System.out.println();
		}
		
		// Calculo das scan lines
		tabelaScanLines = new TreeMap<Integer, List<Borda>>();
		filaCalculo = new LinkedList<Borda>();

		// Obtem primeira linha
		cabeca = tabelaBordas.pollFirstEntry();
		filaCalculo.addAll(cabeca.getValue());
		linhaAtual = cabeca.getKey();
		
		listaBordas = new ArrayList<Borda>();
		for (Borda b: filaCalculo) 
			listaBordas.add(b.copiar());
		tabelaScanLines.put(linhaAtual, (List<Borda>) listaBordas);

		while (filaCalculo.size() > 0) {
			// Incrementa as bordas
			tamanhoFila = filaCalculo.size();
			for (i = 0; i < tamanhoFila; i++) {
				borda = filaCalculo.poll();
				borda = borda.incrementar(linhaAtual, passo);
				if (borda != null)
					filaCalculo.add(borda);
			}

			// Prepara a proxima scan line
			linhaAtual += passo;
			if (tabelaBordas.get(linhaAtual) != null)
				filaCalculo.addAll(tabelaBordas.get(linhaAtual));

			// Constroi uma scan line
			listaBordas = new ArrayList<Borda>();
			listaBordas.addAll(filaCalculo);
			tabelaScanLines.put(linhaAtual, listaBordas);
		}
	
		// Ordenacao
		for (Map.Entry<Integer, List<Borda>> entrada : tabelaScanLines
				.entrySet()) {
			Collections.sort(entrada.getValue());
		}

		segmentoBorda = null;
		// Instanciacao das scan lines
		for (Map.Entry<Integer, List<Borda>> entrada : tabelaScanLines
				.entrySet()) {
			listaBordas = entrada.getValue();
			for (i = 0; i < listaBordas.size()-1; i += 2) {
				borda = listaBordas.get(i);
				borda2 = listaBordas.get(i + 1);
				
				// Otimização
				if (borda.getXint() == borda2.getXint())
					continue;
				if (segmentoBorda != null && segmentoBorda.getPFinal().getX() == borda.getXint()) {
					segmentoBorda.setPFinal(new Ponto(borda2.getXint(), entrada.getKey()));
					segmentoBorda = null;
					continue;
				}
				
				segmentoBorda = new Segmento(new Ponto(
						borda.getXint(), entrada.getKey()), new Ponto(borda2
								.getXint(), entrada.getKey()));

				listaFormasPlotaveis.add(segmentoBorda);
			}
		}
	}

	private int necessitaAjuste(Segmento segmento, Segmento segmento2) {
		Ponto pontoAnterior, ponto, pontoPosterior;
		int retorno = -1;
		
		if (segmento.getPInicial() == segmento2.getPInicial()) {
			ponto = segmento.getPInicial();
			pontoAnterior = segmento.getPFinal();
			pontoPosterior = segmento2.getPFinal();
		}
		else if (segmento.getPInicial() == segmento2.getPFinal()) {
			ponto = segmento.getPInicial();
			pontoAnterior = segmento.getPFinal();
			pontoPosterior = segmento2.getPInicial();
		}
		else if (segmento.getPFinal() == segmento2.getPInicial()) {
			ponto = segmento.getPFinal();
			pontoAnterior = segmento.getPInicial();
			pontoPosterior = segmento2.getPFinal();
		}
		else if (segmento.getPFinal() == segmento2.getPFinal()) {
			ponto = segmento.getPFinal();
			pontoAnterior = segmento.getPInicial();
			pontoPosterior = segmento2.getPInicial();
		}
		else 
			return retorno;
		
		if (pontoAnterior.getY() == ponto.getY() && ponto.getY() < pontoPosterior.getY()){
			retorno = 1;
		}
		else if (ponto.getY() == pontoPosterior.getY() && ponto.getY() < pontoAnterior.getY()){
			retorno = 0;
		}
		else if ((pontoAnterior.getY() < ponto.getY() && ponto.getY() < pontoPosterior.getY())) {
			retorno = 1;
		}
		else if ((pontoAnterior.getY() > ponto.getY() && ponto.getY() > pontoPosterior.getY())) {
			retorno = 0;
		}
		return retorno;
	}

	/**
	 * Constroi as bordas de um poligono
	 * @return
	 */
	private List<Segmento> constroiBordas() {
		// Declaracao de variaveis
		List<Segmento> listaSegmentos;
		Segmento segmento;
		Ponto ponto, pontoAnterior = null;
		int i;

		listaSegmentos = new ArrayList<Segmento>();

		// Constroi os segmentos a partir dos vertices
		for (i = 0; i <= listaVertices.size(); i++) {
			if (i == listaVertices.size())
				ponto = listaVertices.get(0);
			else
				ponto = listaVertices.get(i);

			if (pontoAnterior == null) {
				pontoAnterior = ponto;
				continue;
			}

			segmento = new Segmento(pontoAnterior, ponto);
			listaSegmentos.add(segmento);

			pontoAnterior = ponto;
		}
			
		return listaSegmentos;
	}
	
}
