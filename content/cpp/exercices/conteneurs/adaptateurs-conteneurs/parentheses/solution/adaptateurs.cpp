#include "adaptateurs.hpp"

#include <queue>
#include <stack>

namespace {

char ouvrant_de(char fermant) {
    switch (fermant) {
        case ')':
            return '(';
        case ']':
            return '[';
        default:
            return '{';
    }
}

}  // namespace

bool bien_parenthese(std::string_view texte) {
    std::stack<char> ouverts;
    for (char c : texte) {
        if (c == '(' || c == '[' || c == '{') {
            ouverts.push(c);
        } else if (c == ')' || c == ']' || c == '}') {
            if (ouverts.empty() || ouverts.top() != ouvrant_de(c)) {
                return false;
            }
            ouverts.pop();
        }
    }
    return ouverts.empty();
}

std::vector<int> plus_grands(const std::vector<int>& valeurs, std::size_t k) {
    std::priority_queue<int> tas;
    for (int valeur : valeurs) {
        tas.push(valeur);
    }
    std::vector<int> resultat;
    while (resultat.size() < k && !tas.empty()) {
        resultat.push_back(tas.top());
        tas.pop();
    }
    return resultat;
}
